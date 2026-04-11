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
import kotlinx.coroutines.flow.first
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
    val setupPinError: String? = null,
    val setupPinSuccess: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val canSave: Boolean = false,
    val saveValidationMessage: String? = null,
    val settingsSavedMessage: String? = null,
)

@HiltViewModel
class VaultSettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val billingManager: BillingManager,
) : ViewModel() {

    private data class DraftSettings(
        val fallbackMode: AuthFallbackMode?,
        val lockTimeout: LockTimeout?,
    )

    private data class UiFeedback(
        val decoyPinError: String?,
        val decoyPinSuccess: Boolean,
        val changePinError: String?,
        val changePinSuccess: Boolean,
        val setupPinError: String?,
        val setupPinSuccess: Boolean,
        val settingsSavedMessage: String?,
    )

    private val changePinError = MutableStateFlow<String?>(null)
    private val changePinSuccess = MutableStateFlow(false)
    private val decoyPinError = MutableStateFlow<String?>(null)
    private val decoyPinSuccess = MutableStateFlow(false)
    private val setupPinError = MutableStateFlow<String?>(null)
    private val setupPinSuccess = MutableStateFlow(false)
    private val draftFallbackMode = MutableStateFlow<AuthFallbackMode?>(null)
    private val draftLockTimeout = MutableStateFlow<LockTimeout?>(null)
    private val settingsSavedMessage = MutableStateFlow<String?>(null)

    val uiState = combine(
        authRepository.authPreferences,
        combine(draftFallbackMode, draftLockTimeout) { fallbackDraft, timeoutDraft ->
            DraftSettings(
                fallbackMode = fallbackDraft,
                lockTimeout = timeoutDraft,
            )
        },
        combine(
            combine(decoyPinError, decoyPinSuccess) { decoyError, decoySuccess ->
                decoyError to decoySuccess
            },
            combine(changePinError, changePinSuccess) { pinError, pinSuccess ->
                pinError to pinSuccess
            },
            combine(setupPinError, setupPinSuccess, settingsSavedMessage) { initialPinError, initialPinSuccess, savedMessage ->
                Triple(initialPinError, initialPinSuccess, savedMessage)
            },
        ) { decoyState, changePinState, setupState ->
            UiFeedback(
                decoyPinError = decoyState.first,
                decoyPinSuccess = decoyState.second,
                changePinError = changePinState.first,
                changePinSuccess = changePinState.second,
                setupPinError = setupState.first,
                setupPinSuccess = setupState.second,
                settingsSavedMessage = setupState.third,
            )
        },
    ) { preferences, draftSettings, uiFeedback ->
        val fallbackMode = draftSettings.fallbackMode ?: preferences.fallbackMode
        val lockTimeout = draftSettings.lockTimeout ?: preferences.lockTimeout
        val requiresAppPinSetup = fallbackMode == AuthFallbackMode.APP_PIN && !preferences.hasRealPin
        val hasUnsavedChanges =
            fallbackMode != preferences.fallbackMode || lockTimeout != preferences.lockTimeout

        VaultSettingsUiState(
            fallbackMode = fallbackMode,
            hasAppPin = preferences.hasRealPin,
            isProUnlocked = preferences.isProUnlocked,
            hasDecoyPin = preferences.hasDecoyPin,
            lockTimeout = lockTimeout,
            decoyPinError = uiFeedback.decoyPinError,
            decoyPinSuccess = uiFeedback.decoyPinSuccess,
            changePinError = uiFeedback.changePinError,
            changePinSuccess = uiFeedback.changePinSuccess,
            setupPinError = uiFeedback.setupPinError,
            setupPinSuccess = uiFeedback.setupPinSuccess,
            hasUnsavedChanges = hasUnsavedChanges,
            canSave = hasUnsavedChanges && !requiresAppPinSetup,
            saveValidationMessage = if (requiresAppPinSetup) {
                "Set a LockLens PIN before saving this fallback."
            } else {
                null
            },
            settingsSavedMessage = uiFeedback.settingsSavedMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VaultSettingsUiState(),
    )

    fun selectFallbackMode(mode: AuthFallbackMode) {
        draftFallbackMode.value = mode
        settingsSavedMessage.value = null
    }

    fun setLockTimeout(timeout: LockTimeout) {
        draftLockTimeout.value = timeout
        settingsSavedMessage.value = null
    }

    fun saveSettings() {
        viewModelScope.launch {
            val preferences = authRepository.authPreferences.first()
            val fallbackMode = draftFallbackMode.value ?: preferences.fallbackMode
            val lockTimeout = draftLockTimeout.value ?: preferences.lockTimeout

            if (fallbackMode == AuthFallbackMode.APP_PIN && !preferences.hasRealPin) {
                settingsSavedMessage.value = null
                return@launch
            }

            if (fallbackMode == preferences.fallbackMode && lockTimeout == preferences.lockTimeout) {
                return@launch
            }

            if (fallbackMode != preferences.fallbackMode) {
                authRepository.setFallbackMode(fallbackMode)
            }
            if (lockTimeout != preferences.lockTimeout) {
                authRepository.setLockTimeout(lockTimeout)
            }

            draftFallbackMode.value = null
            draftLockTimeout.value = null
            settingsSavedMessage.value = "Settings saved."
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

    fun setInitialRealPin(pin: String) {
        viewModelScope.launch {
            if (authRepository.isDecoyPin(pin)) {
                setupPinError.value = "LockLens PIN must be different from the decoy PIN."
                setupPinSuccess.value = false
                return@launch
            }

            authRepository.setRealPin(pin)
            setupPinError.value = null
            setupPinSuccess.value = true
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

    fun consumeSetupPinResult() {
        setupPinError.update { null }
        setupPinSuccess.update { false }
    }

    fun launchPurchaseFlow(activity: Activity) {
        billingManager.launchPurchaseFlow(activity)
    }
}
