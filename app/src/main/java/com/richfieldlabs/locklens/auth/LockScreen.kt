package com.richfieldlabs.locklens.auth

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LockScreen(
    onUnlocked: (decoy: Boolean) -> Unit,
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
            val decoy = uiState.decoyUnlocked
            viewModel.consumeUnlockResult()
            onUnlocked(decoy)
        }
    }

    LaunchedEffect(uiState.biometricPromptRequest) {
        if (uiState.biometricPromptRequest == 0) return@LaunchedEffect

        if (activity == null) {
            viewModel.onBiometricUnavailable("Unable to access the Android biometric prompt from this screen.")
            return@LaunchedEffect
        }

        val biometricManager = BiometricManager.from(activity)
        when (biometricManager.canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                launchBiometricPrompt(
                    activity = activity,
                    onSuccess = viewModel::onBiometricAuthenticated,
                    onFailure = viewModel::onBiometricUnavailable,
                )
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                viewModel.onBiometricUnavailable(
                    "Set up fingerprint or face unlock to use biometrics. You can still unlock with your LockLens PIN.",
                )
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            -> {
                viewModel.onBiometricUnavailable(
                    "Biometric unlock is not available on this device right now. Use your LockLens PIN.",
                )
            }
            else -> {
                viewModel.onBiometricUnavailable(
                    "Biometric unlock is temporarily unavailable. Use your LockLens PIN or try again in a moment.",
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "LockLens",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = uiState.setupMessage ?: if (uiState.hasRealPin) {
                    "Enter PIN to unlock"
                } else {
                    "Secure your vault"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // PIN Display
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(6) { index ->
                    val isEntered = index < uiState.enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isEntered) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            uiState.message?.let { message ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message,
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            } ?: Spacer(modifier = Modifier.height(48.dp))

            Spacer(modifier = Modifier.weight(1f))

            // Number Pad
            Column(
                modifier = Modifier.fillMaxWidth(0.8f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf(null, "0", "delete")
                ).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        row.forEach { key ->
                            Box(modifier = Modifier.weight(1f)) {
                                if (key != null) {
                                    if (key == "delete") {
                                        IconButton(
                                            onClick = viewModel::onPinDelete,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    } else {
                                        FilledTonalButton(
                                            onClick = { viewModel.onPinDigitEntered(key) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1f),
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = key,
                                                style = MaterialTheme.typography.headlineMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (uiState.hasRealPin) {
                    TextButton(onClick = viewModel::requestBiometricPrompt) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Biometric")
                    }
                }
                
                if (uiState.enteredPin.length >= 4) {
                    Button(onClick = viewModel::submitPin) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (uiState.hasRealPin) "Unlock" else "Continue")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                ) {
                    return
                }

                onFailure(errString.toString())
            }

            override fun onAuthenticationFailed() {
                onFailure("Authentication not recognized. Try again.")
            }
        },
    )

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock LockLens")
        .setSubtitle("Use fingerprint or face unlock, or choose your LockLens PIN")
        .setAllowedAuthenticators(BIOMETRIC_STRONG)
        .setNegativeButtonText("Use PIN")
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
