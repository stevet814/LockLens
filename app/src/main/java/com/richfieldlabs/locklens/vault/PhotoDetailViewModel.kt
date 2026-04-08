package com.richfieldlabs.locklens.vault

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.richfieldlabs.locklens.crypto.CryptoManager
import com.richfieldlabs.locklens.data.model.Photo
import com.richfieldlabs.locklens.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PhotoDetailUiState(
    val photo: Photo? = null,
    val bitmap: ImageBitmap? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class PhotoDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val photoRepository: PhotoRepository,
    private val cryptoManager: CryptoManager,
) : ViewModel() {

    private val photoId: Long = checkNotNull(savedStateHandle["photoId"])

    private val _uiState = MutableStateFlow(PhotoDetailUiState())
    val uiState: StateFlow<PhotoDetailUiState> = _uiState.asStateFlow()

    init {
        loadPhoto()
    }

    private fun loadPhoto() {
        viewModelScope.launch {
            photoRepository.observePhoto(photoId)
                .filterNotNull()
                .take(1)
                .collect { photo ->
                    _uiState.update { it.copy(photo = photo) }
                    val bitmap = withContext(Dispatchers.IO) {
                        try {
                            val iv = Base64.decode(photo.iv, Base64.NO_WRAP)
                            val bytes = cryptoManager.decrypt(File(photo.encryptedFilePath), iv)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                        } catch (_: Exception) {
                            null
                        }
                    }
                    _uiState.update {
                        it.copy(
                            bitmap = bitmap,
                            isLoading = false,
                            error = if (bitmap == null) "Could not decrypt photo." else null,
                        )
                    }
                }
        }
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
}
