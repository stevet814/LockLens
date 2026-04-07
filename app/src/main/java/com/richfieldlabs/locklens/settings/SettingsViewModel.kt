package com.richfieldlabs.locklens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.richfieldlabs.locklens.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val autoPromptBiometric: Boolean = true,
    val hasDecoyPin: Boolean = false,
    val isProUnlocked: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    val uiState = authRepository.authPreferences
        .map { preferences ->
            SettingsUiState(
                autoPromptBiometric = preferences.biometricEnabled,
                hasDecoyPin = preferences.hasDecoyPin,
                isProUnlocked = preferences.isProUnlocked,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    fun setAutoPromptBiometric(enabled: Boolean) {
        viewModelScope.launch {
            authRepository.setBiometricEnabled(enabled)
        }
    }
}
