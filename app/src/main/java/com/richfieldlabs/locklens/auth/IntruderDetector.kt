package com.richfieldlabs.locklens.auth

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import androidx.annotation.MainThread
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.richfieldlabs.locklens.LockLensApp
import com.richfieldlabs.locklens.R
import com.richfieldlabs.locklens.camera.ExifStripper
import com.richfieldlabs.locklens.crypto.CryptoManager
import com.richfieldlabs.locklens.data.db.IntruderDao
import com.richfieldlabs.locklens.data.model.IntruderEvent
import com.richfieldlabs.locklens.data.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

@Singleton
class IntruderDetector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val cryptoManager: CryptoManager,
    private val intruderDao: IntruderDao,
) {
    companion object {
        const val FAILED_ATTEMPTS_THRESHOLD = 3
        private val notificationId = AtomicInteger(1000)
    }

    /** Called on every failed PIN attempt. Selfie is only captured at the threshold. */
    suspend fun onFailedPinAttempt(attemptedPin: String, capturePhoto: Boolean) {
        val prefs = authRepository.authPreferences.first()
        if (!prefs.isProUnlocked) return

        val (encryptedPath, selfieIv) = if (capturePhoto) {
            captureAndEncryptSelfie() ?: Pair(null, null)
        } else {
            Pair(null, null)
        }

        intruderDao.insert(
            IntruderEvent(
                encryptedSelfieFilePath = encryptedPath,
                selfieIv = selfieIv,
                attemptedPin = authRepository.hashPin(attemptedPin),
            )
        )

        fireIntruderNotification(photoWasCaptured = encryptedPath != null)
    }

    private fun fireIntruderNotification(photoWasCaptured: Boolean) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(context, LockLensApp.INTRUDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Failed vault unlock attempt")
            .setContentText(
                if (photoWasCaptured) "A photo was captured. View it in the intrusion log."
                else "Someone tried to open your vault."
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notificationId.getAndIncrement(), notification)
    }

    private suspend fun captureAndEncryptSelfie(): Pair<String, String>? {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) return null

        return try {
            val cameraProvider = withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { cont ->
                    val future = ProcessCameraProvider.getInstance(context)
                    future.addListener({ cont.resume(future.get()) }, ContextCompat.getMainExecutor(context))
                }
            }

            val lifecycle = withContext(Dispatchers.Main) {
                SingleUseLifecycle().also { it.start() }
            }

            try {
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                withContext(Dispatchers.Main) {
                    cameraProvider.bindToLifecycle(
                        lifecycle,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        imageCapture,
                    )
                }

                val tempFile = File(context.cacheDir, "intruder_${System.currentTimeMillis()}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()

                val capturedFile: File = withContext(Dispatchers.Main) {
                    suspendCancellableCoroutine<File?> { cont ->
                        imageCapture.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) =
                                    cont.resume(tempFile)

                                override fun onError(exception: ImageCaptureException) =
                                    cont.resume(null)
                            }
                        )
                    }
                } ?: return null

                withContext(Dispatchers.IO) {
                    ExifStripper.strip(capturedFile)

                    val intrudersDir = File(context.filesDir, "intruders").apply { mkdirs() }
                    val encFile = File(intrudersDir, "${UUID.randomUUID()}.enc")
                    val iv = capturedFile.inputStream().use { stream -> cryptoManager.encrypt(stream, encFile) }
                    capturedFile.delete()

                    Pair(encFile.absolutePath, Base64.encodeToString(iv, Base64.NO_WRAP))
                }
            } finally {
                withContext(Dispatchers.Main) {
                    lifecycle.destroy()
                    cameraProvider.unbindAll()
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private class SingleUseLifecycle : LifecycleOwner {
        private val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle = registry

        @MainThread
        fun start() {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        @MainThread
        fun destroy() {
            if (lifecycle.currentState != Lifecycle.State.DESTROYED) {
                registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
                registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            }
        }
    }
}
