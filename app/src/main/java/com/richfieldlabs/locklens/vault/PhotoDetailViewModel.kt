package com.richfieldlabs.locklens.vault

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.webkit.MimeTypeMap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.richfieldlabs.locklens.billing.BillingManager
import com.richfieldlabs.locklens.crypto.CryptoManager
import com.richfieldlabs.locklens.data.model.Photo
import com.richfieldlabs.locklens.data.repository.AuthRepository
import com.richfieldlabs.locklens.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PhotoDetailUiState(
    val photo: Photo? = null,
    val bitmap: ImageBitmap? = null,
    val videoUri: Uri? = null,
    val isLoading: Boolean = true,
    val isProUnlocked: Boolean = false,
    val isVideoLocked: Boolean = false,
    val shareUri: Uri? = null,
    val error: String? = null,
)

@HiltViewModel
class PhotoDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val photoRepository: PhotoRepository,
    private val cryptoManager: CryptoManager,
    private val authRepository: AuthRepository,
    private val billingManager: BillingManager,
) : ViewModel() {

    private val photoId: Long = checkNotNull(savedStateHandle["photoId"])

    private val _uiState = MutableStateFlow(PhotoDetailUiState())
    val uiState: StateFlow<PhotoDetailUiState> = _uiState.asStateFlow()

    private var tempVideoFile: File? = null
    private var tempShareFile: File? = null

    init {
        loadPhoto()
        observeProState()
    }

    private fun observeProState() {
        viewModelScope.launch {
            authRepository.authPreferences.collect { prefs ->
                val photo = _uiState.value.photo
                val shouldUnlockVideo = prefs.isProUnlocked &&
                    _uiState.value.isVideoLocked &&
                    photo?.mimeType?.startsWith("video/") == true

                _uiState.update {
                    it.copy(
                        isProUnlocked = prefs.isProUnlocked,
                        isVideoLocked = photo?.mimeType?.startsWith("video/") == true && !prefs.isProUnlocked,
                    )
                }

                if (shouldUnlockVideo) {
                    val currentPhoto = photo ?: return@collect
                    _uiState.update {
                        it.copy(
                            isLoading = true,
                            isVideoLocked = false,
                            error = null,
                        )
                    }
                    loadVideo(currentPhoto)
                }
            }
        }
    }

    private fun loadPhoto() {
        viewModelScope.launch {
            photoRepository.observePhoto(photoId)
                .filterNotNull()
                .take(1)
                .collect { photo ->
                    val isProUnlocked = authRepository.authPreferences.first().isProUnlocked
                    val isVideo = photo.mimeType.startsWith("video/")

                    _uiState.update {
                        it.copy(
                            photo = photo,
                            isProUnlocked = isProUnlocked,
                            isVideoLocked = isVideo && !isProUnlocked,
                        )
                    }

                    if (isVideo && !isProUnlocked) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                videoUri = null,
                                isVideoLocked = true,
                                error = null,
                            )
                        }
                    } else if (isVideo) {
                        loadVideo(photo)
                    } else {
                        loadImage(photo)
                    }
                }
        }
    }

    private suspend fun loadImage(photo: Photo) {
        val bitmap = withContext(Dispatchers.IO) {
            try {
                val iv = Base64.decode(photo.iv, Base64.NO_WRAP)
                val bytes = cryptoManager.decrypt(File(photo.encryptedFilePath), iv)
                try {
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                } finally {
                    bytes.fill(0)
                }
            } catch (_: Exception) {
                null
            }
        }
        _uiState.update {
            it.copy(
                bitmap = bitmap,
                videoUri = null,
                isLoading = false,
                isVideoLocked = false,
                error = if (bitmap == null) "Could not decrypt photo." else null,
            )
        }
    }

    private suspend fun loadVideo(photo: Photo) {
        val uri = withContext(Dispatchers.IO) {
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(photo.mimeType) ?: "mp4"
            val outFile = File(context.cacheDir, "video_${photoId}.$ext")
            try {
                val iv = Base64.decode(photo.iv, Base64.NO_WRAP)
                val bytes = cryptoManager.decrypt(File(photo.encryptedFilePath), iv)
                try {
                    outFile.writeBytes(bytes)
                } finally {
                    bytes.fill(0)
                }
                tempVideoFile = outFile
                Uri.fromFile(outFile)
            } catch (_: Exception) {
                outFile.delete()  // Remove partial file on any failure
                null
            }
        }
        _uiState.update {
            it.copy(
                bitmap = null,
                videoUri = uri,
                isLoading = false,
                isVideoLocked = false,
                error = if (uri == null) "Could not decrypt video." else null,
            )
        }
    }

    /** Decrypts the current item to cacheDir/shared/ and emits a shareable URI. */
    fun prepareShare() {
        if (!_uiState.value.isProUnlocked) return
        val photo = _uiState.value.photo ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(photo.mimeType) ?: "jpg"
                val shareDir = File(context.cacheDir, "shared").apply { mkdirs() }
                val outFile = File(shareDir, "share_${System.currentTimeMillis()}.$ext")
                val iv = Base64.decode(photo.iv, Base64.NO_WRAP)
                val bytes = cryptoManager.decrypt(File(photo.encryptedFilePath), iv)
                try {
                    outFile.writeBytes(bytes)
                } finally {
                    bytes.fill(0)
                }

                tempShareFile?.delete()
                tempShareFile = outFile

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    outFile,
                )
                _uiState.update { it.copy(shareUri = uri) }
            } catch (_: Exception) {
                // share silently fails — no UI feedback needed
            }
        }
    }

    /** Called after the share intent is launched so the temp file is cleaned up. */
    fun onShareLaunched() {
        _uiState.update { it.copy(shareUri = null) }
        tempShareFile?.delete()
        tempShareFile = null
    }

    fun launchPurchaseFlow(activity: Activity) {
        billingManager.launchPurchaseFlow(activity)
    }

    fun deletePhoto(onDeleted: () -> Unit) {
        val photo = _uiState.value.photo ?: return
        viewModelScope.launch(Dispatchers.IO) {
            File(photo.encryptedFilePath).delete()
            File(photo.encryptedThumbPath).delete()
            photoRepository.delete(photo)
            withContext(Dispatchers.Main) { onDeleted() }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tempVideoFile?.delete()
        tempShareFile?.delete()
    }
}
