package com.richfieldlabs.locklens.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.richfieldlabs.locklens.crypto.CryptoManager
import com.richfieldlabs.locklens.data.model.Photo
import com.richfieldlabs.locklens.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel
class CameraViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager,
    private val photoRepository: PhotoRepository,
) : ViewModel() {
    fun saveToVault(tempFile: File, albumId: Long? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val photoDirectory = File(context.filesDir, "vault/photos").apply { mkdirs() }
            val thumbDirectory = File(context.filesDir, "vault/thumbs").apply { mkdirs() }

            try {
                val encryptedPhoto = File(photoDirectory, "${UUID.randomUUID()}.enc")
                val encryptedThumb = File(thumbDirectory, "${UUID.randomUUID()}.enc")

                val photoIv = tempFile.inputStream().use { input ->
                    cryptoManager.encrypt(input, encryptedPhoto)
                }

                val thumbnailBytes = generateThumbnailBytes(tempFile)
                val thumbIv = ByteArrayInputStream(thumbnailBytes).use { input ->
                    cryptoManager.encrypt(input, encryptedThumb)
                }

                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(tempFile.absolutePath, bounds)

                photoRepository.insert(
                    Photo(
                        albumId = albumId,
                        encryptedFilePath = encryptedPhoto.absolutePath,
                        encryptedThumbPath = encryptedThumb.absolutePath,
                        iv = Base64.encodeToString(photoIv, Base64.NO_WRAP),
                        thumbIv = Base64.encodeToString(thumbIv, Base64.NO_WRAP),
                        mimeType = "image/jpeg",
                        originalWidth = bounds.outWidth.coerceAtLeast(0),
                        originalHeight = bounds.outHeight.coerceAtLeast(0),
                        capturedAt = System.currentTimeMillis(),
                    ),
                )
            } finally {
                tempFile.delete()
            }
        }
    }

    private fun generateThumbnailBytes(sourceFile: File): ByteArray {
        val source = BitmapFactory.decodeFile(sourceFile.absolutePath)
            ?: error("Unable to decode source image for thumbnail generation.")
        val thumbSize = 256
        val scaled = Bitmap.createScaledBitmap(source, thumbSize, thumbSize, true)
        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 88, output)
        source.recycle()
        scaled.recycle()
        return output.toByteArray()
    }
}
