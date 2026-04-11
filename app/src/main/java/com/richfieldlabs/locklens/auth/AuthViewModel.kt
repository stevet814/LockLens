package com.richfieldlabs.locklens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.richfieldlabs.locklens.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isOnboarding: Boolean = true,
    val hasRealPin: Boolean = false,
    val hasDecoyPin: Boolean = false,
    val autoPromptBiometric: Boolean = true,
    val fallbackMode: AuthFallbackMode = AuthFallbackMode.DEVICE_CREDENTIAL,
    val message: String? = null,
    val unlockResult: Boolean = false,
    val decoyUnlocked: Boolean = false,
    val biometricPromptRequest: Int = 0,
    val enteredPin: String = "",
    val isConfirmingPin: Boolean = false,
    val setupMessage: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val intruderDetector: IntruderDetector,
) : ViewModel() {
    private val message = MutableStateFlow<String?>(null)
    private val unlockResult = MutableStateFlow(false)
    private val decoyUnlocked = MutableStateFlow(false)
    private val biometricPromptRequest = MutableStateFlow(0)
    private val enteredPin = MutableStateFlow("")
    private val isConfirmingPin = MutableStateFlow(false)
    private var firstPinAttempt = ""

    private data class MutableSnapshot(
        val notice: String?,
        val unlockResult: Boolean,
        val decoyUnlocked: Boolean,
        val biometricPromptRequest: Int,
    )

    val uiState = combine(
        authRepository.authPreferences,
        combine(message, unlockResult, decoyUnlocked, biometricPromptRequest) { m, r, d, p ->
            MutableSnapshot(m, r, d, p)
        },
        enteredPin,
        isConfirmingPin,
    ) { preferences, snap, pin, confirming ->
        AuthUiState(
            isOnboarding = !preferences.isVaultInitialized,
            hasRealPin = preferences.hasRealPin,
            hasDecoyPin = preferences.hasDecoyPin,
            autoPromptBiometric = preferences.biometricEnabled,
            fallbackMode = preferences.fallbackMode,
            message = snap.notice,
            unlockResult = snap.unlockResult,
            decoyUnlocked = snap.decoyUnlocked,
            biometricPromptRequest = snap.biometricPromptRequest,
            enteredPin = pin,
            isConfirmingPin = confirming,
            setupMessage = when {
                preferences.fallbackMode == AuthFallbackMode.APP_PIN && !preferences.hasRealPin && !confirming ->
                    "Create a master PIN to protect your vault."
                preferences.fallbackMode == AuthFallbackMode.APP_PIN && !preferences.hasRealPin && confirming ->
                    "Confirm your master PIN."
                else -> null
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AuthUiState(),
    )

    fun onScreenShown() {
        viewModelScope.launch {
            val prefs = authRepository.authPreferences.first()
            if (prefs.hasRealPin) {
                val remainingLockoutMillis = authRepository.getRemainingPinLockoutMillis()
                if (remainingLockoutMillis > 0L) {
                    message.value = PinLockoutPolicy.buildMessage(remainingLockoutMillis)
                }
            }
            if (prefs.biometricEnabled &&
                (prefs.fallbackMode == AuthFallbackMode.DEVICE_CREDENTIAL || prefs.hasRealPin)
            ) {
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
            authRepository.resetFailedAttempts()
            authRepository.markVaultInitialized()
            unlockResult.value = true
            decoyUnlocked.value = false
        }
    }

    fun onBiometricUnavailable(message: String) {
        this.message.value = message
    }

    fun onPinDigitEntered(digit: String) {
        if (enteredPin.value.length < 6) {
            enteredPin.update { it + digit }
            if (enteredPin.value.length >= 4) {
                if (enteredPin.value.length == 6) {
                    submitPin()
                }
            }
        }
    }

    fun onPinDelete() {
        enteredPin.update { if (it.isNotEmpty()) it.dropLast(1) else it }
    }

    fun submitPin() {
        val pin = enteredPin.value
        if (pin.length < 4) {
            message.value = "PIN must be at least 4 digits."
            return
        }

        viewModelScope.launch {
            val prefs = authRepository.authPreferences.first()
            if (!prefs.hasRealPin) {
                handlePinSetup(pin)
                return@launch
            }

            val remainingLockoutMillis = authRepository.getRemainingPinLockoutMillis()
            if (remainingLockoutMillis > 0L) {
                enteredPin.value = ""
                message.value = PinLockoutPolicy.buildMessage(remainingLockoutMillis)
                return@launch
            }

            handlePinUnlock(pin)
        }
    }

    private suspend fun handlePinSetup(pin: String) {
        if (!isConfirmingPin.value) {
            firstPinAttempt = pin
            enteredPin.value = ""
            isConfirmingPin.value = true
            message.value = null
        } else {
            if (pin == firstPinAttempt) {
                authRepository.resetFailedAttempts()
                authRepository.setRealPin(pin)
                authRepository.markVaultInitialized()
                unlockResult.value = true
                decoyUnlocked.value = false
                message.value = "PIN set successfully."
            } else {
                message.value = "PINs do not match. Try again."
                enteredPin.value = ""
                isConfirmingPin.value = false
                firstPinAttempt = ""
            }
        }
    }

    private suspend fun handlePinUnlock(pin: String) {
        when (authRepository.checkPin(pin)) {
            PinResult.REAL -> {
                authRepository.resetFailedAttempts()
                unlockResult.value = true
                decoyUnlocked.value = false
                enteredPin.value = ""
            }
            PinResult.DECOY -> {
                authRepository.resetFailedAttempts()
                unlockResult.value = true
                decoyUnlocked.value = true
                enteredPin.value = ""
            }
            PinResult.WRONG -> {
                val failedAttempt = authRepository.registerFailedPinAttempt()
                val remainder = failedAttempt.totalAttempts % IntruderDetector.FAILED_ATTEMPTS_THRESHOLD
                val baseMessage = "Incorrect PIN."
                val lockoutMessage = if (failedAttempt.lockoutRemainingMillis > 0L) {
                    " ${PinLockoutPolicy.buildMessage(failedAttempt.lockoutRemainingMillis)}"
                } else {
                    ""
                }
                message.value = baseMessage + lockoutMessage
                enteredPin.value = ""
                val capturePhoto = remainder == 0
                viewModelScope.launch(Dispatchers.IO) {
                    intruderDetector.onFailedPinAttempt(pin, capturePhoto)
                }
            }
            PinResult.UNSET -> {
                message.value = "PIN not set."
            }
        }
    }

    fun consumeUnlockResult() {
        unlockResult.value = false
        decoyUnlocked.value = false
    }
}
