package com.richfieldlabs.locklens.auth

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.richfieldlabs.locklens.ui.components.EmptyState

private const val ALLOWED_AUTHENTICATORS = BIOMETRIC_STRONG or DEVICE_CREDENTIAL

@Composable
fun LockScreen(
    onUnlocked: (decoy: Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findFragmentActivity()

    LaunchedEffect(Unit) {
        viewModel.onScreenShown()
    }

    LaunchedEffect(uiState.unlockResult) {
        if (uiState.unlockResult) {
            viewModel.consumeUnlockResult()
            onUnlocked(false)
        }
    }

    LaunchedEffect(uiState.biometricPromptRequest) {
        if (uiState.biometricPromptRequest == 0) return@LaunchedEffect

        if (activity == null) {
            viewModel.onBiometricUnavailable("Unable to access the Android biometric prompt from this screen.")
            return@LaunchedEffect
        }

        val biometricManager = BiometricManager.from(activity)
        when (biometricManager.canAuthenticate(ALLOWED_AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                launchBiometricPrompt(
                    activity = activity,
                    onSuccess = viewModel::onBiometricAuthenticated,
                    onFailure = viewModel::onBiometricUnavailable,
                )
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                viewModel.onBiometricUnavailable(
                    "Set up fingerprint, face unlock, or a device screen lock to open the vault.",
                )
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            -> {
                viewModel.onBiometricUnavailable(
                    "This device does not currently have biometric or device credential auth available.",
                )
            }
            else -> {
                viewModel.onBiometricUnavailable(
                    "Android auth is temporarily unavailable. Try again in a moment.",
                )
            }
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "LockLens",
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    text = if (uiState.isOnboarding) {
                        "Use Android biometrics or your device screen lock to initialize the vault."
                    } else {
                        "Unlock with Android biometrics or your device screen lock."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                uiState.message?.let { message ->
                    Card {
                        Text(
                            text = message,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                EmptyState(
                    icon = Icons.Default.Fingerprint,
                    title = if (uiState.isOnboarding) {
                        "Device-protected vault"
                    } else {
                        "Ready to unlock"
                    },
                    body = if (uiState.isOnboarding) {
                        "No custom app PIN required. LockLens can use the same biometric or screen lock trust that Android already manages."
                    } else {
                        "Tap below if the prompt does not appear automatically."
                    },
                )
                Button(
                    onClick = viewModel::requestBiometricPrompt,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Unlock with biometrics")
                }
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Open settings")
                }
            }

            Text(
                text = "The camera that forgets where you were.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun launchBiometricPrompt(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit,
) {
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                ) {
                    onFailure("Authentication canceled.")
                } else {
                    onFailure(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                onFailure("Authentication not recognized. Try again.")
            }
        },
    )

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock LockLens")
        .setSubtitle("Use your Android biometric or device screen lock")
        .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
        .build()

    prompt.authenticate(promptInfo)
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? {
    return when (this) {
        is FragmentActivity -> this
        is ContextWrapper -> baseContext.findFragmentActivity()
        else -> null
    }
}
