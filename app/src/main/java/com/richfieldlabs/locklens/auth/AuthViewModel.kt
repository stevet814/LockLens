package com.richfieldlabs.locklens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.richfieldlabs.locklens.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isOnboarding: Boolean = true,
    val hasDecoyPin: Boolean = false,
    val autoPromptBiometric: Boolean = true,
    val message: String? = null,
    val unlockResult: Boolean = false,
    val biometricPromptRequest: Int = 0,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val message = MutableStateFlow<String?>(null)
    private val unlockResult = MutableStateFlow(false)
    private val biometricPromptRequest = MutableStateFlow(0)

    val uiState = combine(
        authRepository.authPreferences,
        message,
        unlockResult,
        biometricPromptRequest,
    ) { preferences, notice, result, promptRequest ->
        AuthUiState(
            isOnboarding = !preferences.isVaultInitialized,
            hasDecoyPin = preferences.hasDecoyPin,
            autoPromptBiometric = preferences.biometricEnabled,
            message = notice ?: run {
                if (!preferences.isVaultInitialized) {
                    "Use your device biometric or screen lock to protect this vault on this phone."
                } else {
                    "Unlock with your device biometric or screen lock."
                }
            },
            unlockResult = result,
            biometricPromptRequest = promptRequest,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AuthUiState(),
    )

    fun onScreenShown() {
        viewModelScope.launch {
            if (authRepository.authPreferences.first().biometricEnabled) {
                requestBiometricPrompt()
            }
        }
    }

    fun requestBiometricPrompt() {
        message.value = null
        biometricPromptRequest.update { it + 1 }
    }

    fun onBiometricAuthenticated() {
        viewModelScope.launch {
            authRepository.markVaultInitialized()
            unlockResult.value = true
        }
    }

    fun onBiometricUnavailable(message: String) {
        this.message.value = message
    }

    fun consumeUnlockResult() {
        unlockResult.value = false
    }

    fun setAutoPromptBiometric(enabled: Boolean) {
        viewModelScope.launch {
            authRepository.setBiometricEnabled(enabled)
        }
    }
}
