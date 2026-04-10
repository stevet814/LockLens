package com.richfieldlabs.locklens.vault

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.richfieldlabs.locklens.auth.AuthFallbackMode
import com.richfieldlabs.locklens.auth.LockTimeout
import com.richfieldlabs.locklens.auth.PinResult
import com.richfieldlabs.locklens.billing.BillingManager
import com.richfieldlabs.locklens.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VaultSettingsUiState(
    val fallbackMode: AuthFallbackMode = AuthFallbackMode.DEVICE_CREDENTIAL,
    val hasAppPin: Boolean = false,
    val isProUnlocked: Boolean = false,
    val hasDecoyPin: Boolean = false,
    val lockTimeout: LockTimeout = LockTimeout.IMMEDIATE,
    val decoyPinError: String? = null,
    val decoyPinSuccess: Boolean = false,
    val changePinError: String? = null,
    val changePinSuccess: Boolean = false,
)

@HiltViewModel
class VaultSettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val billingManager: BillingManager,
) : ViewModel() {

    private val changePinError = MutableStateFlow<String?>(null)
    private val changePinSuccess = MutableStateFlow(false)
    private val decoyPinError = MutableStateFlow<String?>(null)
    private val decoyPinSuccess = MutableStateFlow(false)

    val uiState = combine(
        authRepository.authPreferences,
        decoyPinError,
        decoyPinSuccess,
        changePinError,
        changePinSuccess,
    ) { preferences, decoyError, decoySuccess, pinError, pinSuccess ->
        VaultSettingsUiState(
            fallbackMode = preferences.fallbackMode,
            hasAppPin = preferences.hasRealPin,
            isProUnlocked = preferences.isProUnlocked,
            hasDecoyPin = preferences.hasDecoyPin,
            lockTimeout = preferences.lockTimeout,
            decoyPinError = decoyError,
            decoyPinSuccess = decoySuccess,
            changePinError = pinError,
            changePinSuccess = pinSuccess,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VaultSettingsUiState(),
    )

    fun selectFallbackMode(mode: AuthFallbackMode) {
        viewModelScope.launch {
            authRepository.setFallbackMode(mode)
        }
    }

    fun setLockTimeout(timeout: LockTimeout) {
        viewModelScope.launch {
            authRepository.setLockTimeout(timeout)
        }
    }

    fun setDecoyPin(pin: String) {
        viewModelScope.launch {
            if (authRepository.isRealPin(pin)) {
                decoyPinError.value = "Decoy PIN must be different from your LockLens PIN."
                decoyPinSuccess.value = false
                return@launch
            }

            authRepository.setDecoyPin(pin)
            decoyPinError.value = null
            decoyPinSuccess.value = true
        }
    }

    fun changeRealPin(currentPin: String, newPin: String) {
        viewModelScope.launch {
            when (authRepository.checkPin(currentPin)) {
                PinResult.REAL -> {
                    if (authRepository.isDecoyPin(newPin)) {
                        changePinError.value = "LockLens PIN must be different from the decoy PIN."
                        changePinSuccess.value = false
                        return@launch
                    }
                    authRepository.setRealPin(newPin)
                    changePinSuccess.value = true
                    changePinError.value = null
                }
                else -> {
                    changePinError.value = "Current PIN is incorrect."
                    changePinSuccess.value = false
                }
            }
        }
    }

    fun consumeChangePinResult() {
        changePinError.update { null }
        changePinSuccess.update { false }
    }

    fun consumeDecoyPinResult() {
        decoyPinError.update { null }
        decoyPinSuccess.update { false }
    }

    fun launchPurchaseFlow(activity: Activity) {
        billingManager.launchPurchaseFlow(activity)
    }
}
