package com.richfieldlabs.locklens.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.richfieldlabs.locklens.auth.AuthFallbackMode
import com.richfieldlabs.locklens.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VaultSettingsUiState(
    val fallbackMode: AuthFallbackMode = AuthFallbackMode.DEVICE_CREDENTIAL,
    val hasAppPin: Boolean = false,
)

@HiltViewModel
class VaultSettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    val uiState = authRepository.authPreferences
        .map { preferences ->
            VaultSettingsUiState(
                fallbackMode = preferences.fallbackMode,
                hasAppPin = preferences.hasRealPin,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = VaultSettingsUiState(),
        )

    fun selectFallbackMode(mode: AuthFallbackMode) {
        viewModelScope.launch {
            authRepository.setFallbackMode(mode)
        }
    }
}
