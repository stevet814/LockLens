package com.richfieldlabs.locklens.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.richfieldlabs.locklens.data.model.Photo
import com.richfieldlabs.locklens.data.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class VaultUiState(
    val photos: List<Photo> = emptyList(),
)

@HiltViewModel
class VaultViewModel @Inject constructor(
    photoRepository: PhotoRepository,
) : ViewModel() {
    val uiState = photoRepository.observePhotos()
        .map(::VaultUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = VaultUiState(),
        )
}

