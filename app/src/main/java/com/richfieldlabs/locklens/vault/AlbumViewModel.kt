package com.richfieldlabs.locklens.vault

import android.util.Base64
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.richfieldlabs.locklens.crypto.CryptoManager
import com.richfieldlabs.locklens.data.model.Photo
import com.richfieldlabs.locklens.data.repository.AlbumRepository
import com.richfieldlabs.locklens.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AlbumUiState(
    val albumName: String = "",
    val photos: List<Photo> = emptyList(),
)

@HiltViewModel
class AlbumViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val photoRepository: PhotoRepository,
    private val albumRepository: AlbumRepository,
    private val cryptoManager: CryptoManager,
) : ViewModel() {

    private val albumId: Long = checkNotNull(savedStateHandle["albumId"])

    val uiState = combine(
        albumRepository.observeById(albumId),
        photoRepository.observeAlbum(albumId),
    ) { album, photos ->
        AlbumUiState(
            albumName = album?.name ?: "Album",
            photos = photos,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AlbumUiState(),
    )

    fun decryptThumbnail(photo: Photo): ByteArray {
        val iv = Base64.decode(photo.thumbIv, Base64.NO_WRAP)
        return cryptoManager.decrypt(File(photo.encryptedThumbPath), iv)
    }

    fun deletePhoto(photo: Photo) {
        viewModelScope.launch(Dispatchers.IO) {
            File(photo.encryptedFilePath).delete()
            File(photo.encryptedThumbPath).delete()
            photoRepository.delete(photo)
        }
    }
}
