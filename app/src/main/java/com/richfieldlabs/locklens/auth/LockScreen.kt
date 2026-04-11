package com.richfieldlabs.locklens.auth

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.richfieldlabs.locklens.R

private const val DEVICE_CREDENTIAL_AUTHENTICATORS = BIOMETRIC_STRONG or DEVICE_CREDENTIAL

@Composable
fun LockScreen(
    onUnlocked: (decoy: Boolean) -> Unit,
    onOpenDeviceSecuritySettings: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findFragmentActivity()
    val canSubmitPin = uiState.enteredPin.length >= 4
    val primaryPinActionLabel = when {
        uiState.hasRealPin -> "Unlock"
        uiState.isConfirmingPin -> "Confirm PIN"
        else -> "Continue"
    }

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
        val allowedAuthenticators = when (uiState.fallbackMode) {
            AuthFallbackMode.DEVICE_CREDENTIAL -> DEVICE_CREDENTIAL_AUTHENTICATORS
            AuthFallbackMode.APP_PIN -> BIOMETRIC_STRONG
        }

        when (biometricManager.canAuthenticate(allowedAuthenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                launchBiometricPrompt(
                    activity = activity,
                    fallbackMode = uiState.fallbackMode,
                    onSuccess = viewModel::onBiometricAuthenticated,
                    onFailure = viewModel::onBiometricUnavailable,
                )
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                viewModel.onBiometricUnavailable(
                    if (uiState.fallbackMode == AuthFallbackMode.DEVICE_CREDENTIAL) {
                        "Set up a screen lock or biometric on this device to unlock LockLens."
                    } else {
                        "Set up fingerprint or face unlock to use biometrics. You can still unlock with your LockLens PIN."
                    },
                )
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            -> {
                viewModel.onBiometricUnavailable(
                    if (uiState.fallbackMode == AuthFallbackMode.DEVICE_CREDENTIAL) {
                        "Device credential unlock is not available right now. Check your phone security settings."
                    } else {
                        "Biometric unlock is not available on this device right now. Use your LockLens PIN."
                    },
                )
            }
            else -> {
                viewModel.onBiometricUnavailable(
                    if (uiState.fallbackMode == AuthFallbackMode.DEVICE_CREDENTIAL) {
                        "Device authentication is temporarily unavailable. Try again in a moment."
                    } else {
                        "Biometric unlock is temporarily unavailable. Use your LockLens PIN or try again in a moment."
                    },
                )
            }
        }
    }

    Scaffold { innerPadding ->
        val availableHeight = (
            LocalConfiguration.current.screenHeightDp.dp -
                innerPadding.calculateTopPadding() -
                innerPadding.calculateBottomPadding()
            ).coerceAtLeast(0.dp)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isCompactHeight = availableHeight < 900.dp
            val isVeryCompactHeight = availableHeight < 780.dp
            val scrollState = rememberScrollState()
            val topSpacer = if (isVeryCompactHeight) 8.dp else if (isCompactHeight) 16.dp else 24.dp
            val logoSize = if (isVeryCompactHeight) 72.dp else if (isCompactHeight) 80.dp else 88.dp
            val logoCornerRadius = if (isCompactHeight) 20.dp else 24.dp
            val titleStyle = if (isCompactHeight) {
                MaterialTheme.typography.headlineMedium
            } else {
                MaterialTheme.typography.headlineLarge
            }
            val subtitleStyle = if (isCompactHeight) {
                MaterialTheme.typography.bodyMedium
            } else {
                MaterialTheme.typography.bodyLarge
            }
            val subtitleToPinGap = if (isCompactHeight) 20.dp else 32.dp
            val pinIndicatorSize = if (isCompactHeight) 14.dp else 16.dp
            val pinIndicatorGap = if (isCompactHeight) 10.dp else 12.dp
            val emptyMessageHeight = if (isCompactHeight) 20.dp else 48.dp
            val contentGapBeforePad = if (isCompactHeight) 16.dp else 24.dp
            val keypadGap = if (isCompactHeight) 12.dp else 16.dp
            val keypadButtonSize = when {
                availableHeight < 760.dp -> 76.dp
                availableHeight < 900.dp -> 84.dp
                else -> 92.dp
            }
            val keypadWidth = (keypadButtonSize * 3) + (keypadGap * 2)
            val actionButtonHeight = if (isCompactHeight) 48.dp else 56.dp
            val actionButtonShape = RoundedCornerShape(if (isCompactHeight) 18.dp else 20.dp)
            val actionGap = if (isCompactHeight) 8.dp else 12.dp
            val contentModifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(
                    horizontal = if (isCompactHeight) 20.dp else 24.dp,
                    vertical = if (isVeryCompactHeight) 12.dp else 20.dp,
                )
                .then(
                    if (isCompactHeight) {
                        Modifier.verticalScroll(scrollState)
                    } else {
                        Modifier
                    }
                )

            Column(
                modifier = contentModifier,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(topSpacer))

                Box(
                    modifier = Modifier
                        .size(logoSize)
                        .clip(RoundedCornerShape(logoCornerRadius))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.locklens_mark),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(1.35f),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
                    )
                }

                Spacer(modifier = Modifier.height(if (isCompactHeight) 14.dp else 20.dp))

                Text(
                    text = "LockLens",
                    style = titleStyle,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.height(if (isCompactHeight) 6.dp else 8.dp))

                Text(
                    text = uiState.setupMessage ?: when {
                        uiState.fallbackMode == AuthFallbackMode.DEVICE_CREDENTIAL ->
                            "Use your phone security to unlock"
                        uiState.hasRealPin -> "Enter PIN to unlock"
                        else -> "Secure your vault"
                    },
                    style = subtitleStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(subtitleToPinGap))

                if (uiState.fallbackMode == AuthFallbackMode.APP_PIN) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(pinIndicatorGap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        repeat(6) { index ->
                            val isEntered = index < uiState.enteredPin.length
                            Box(
                                modifier = Modifier
                                    .size(pinIndicatorSize)
                                    .clip(CircleShape)
                                    .background(
                                        if (isEntered) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isCompactHeight) 12.dp else 16.dp))

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
                } ?: Spacer(modifier = Modifier.height(emptyMessageHeight))

                if (isCompactHeight) {
                    Spacer(modifier = Modifier.height(contentGapBeforePad))
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (uiState.fallbackMode == AuthFallbackMode.APP_PIN) {
                    Column(
                        modifier = Modifier.width(keypadWidth),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(keypadGap),
                    ) {
                        listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf(null, "0", "delete")
                        ).forEach { row ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(keypadGap),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                row.forEach { key ->
                                    if (key == null) {
                                        Spacer(modifier = Modifier.size(keypadButtonSize))
                                    } else if (key == "delete") {
                                        FilledTonalButton(
                                            onClick = viewModel::onPinDelete,
                                            enabled = uiState.enteredPin.isNotEmpty(),
                                            modifier = Modifier.size(keypadButtonSize),
                                            shape = CircleShape,
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.onSurface,
                                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            ),
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                contentDescription = "Delete",
                                                modifier = Modifier.size(if (isCompactHeight) 24.dp else 28.dp),
                                            )
                                        }
                                    } else {
                                        FilledTonalButton(
                                            onClick = { viewModel.onPinDigitEntered(key) },
                                            modifier = Modifier.size(keypadButtonSize),
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = key,
                                                style = if (isCompactHeight) {
                                                    MaterialTheme.typography.titleLarge
                                                } else {
                                                    MaterialTheme.typography.headlineMedium
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        AuthSummaryCard()
                        OutlinedButton(onClick = onOpenDeviceSecuritySettings) {
                            Text("Open device security settings")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isCompactHeight) 16.dp else 24.dp))

                if (uiState.fallbackMode == AuthFallbackMode.APP_PIN) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(actionGap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (uiState.hasRealPin) {
                            if (isCompactHeight) {
                                FilledTonalIconButton(
                                    onClick = viewModel::requestBiometricPrompt,
                                    modifier = Modifier.size(actionButtonHeight),
                                    shape = actionButtonShape,
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.primary,
                                    ),
                                ) {
                                    Icon(
                                        Icons.Default.Fingerprint,
                                        contentDescription = "Use biometrics",
                                        modifier = Modifier.size(26.dp),
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = viewModel::requestBiometricPrompt,
                                    modifier = Modifier
                                        .width(156.dp)
                                        .height(actionButtonHeight),
                                    shape = actionButtonShape,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary,
                                    ),
                                ) {
                                    Icon(
                                        Icons.Default.Fingerprint,
                                        contentDescription = "Use biometrics",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Biometric")
                                }
                            }
                        }

                        Button(
                            onClick = viewModel::submitPin,
                            enabled = canSubmitPin,
                            modifier = Modifier
                                .weight(1f)
                                .height(actionButtonHeight),
                            shape = actionButtonShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onBackground,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        ) {
                            if (canSubmitPin) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(primaryPinActionLabel)
                        }
                    }
                } else {
                    Button(
                        onClick = viewModel::requestBiometricPrompt,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (uiState.isOnboarding) "Secure with device credential"
                            else "Unlock with device security"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 16.dp))
            }
        }
    }
}

@Composable
private fun AuthSummaryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Unlock with device security",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            HorizontalDivider()
            Text(
                text = "Use the same PIN, pattern, or password you use to unlock your phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun launchBiometricPrompt(
    activity: FragmentActivity,
    fallbackMode: AuthFallbackMode,
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

    val promptInfo = when (fallbackMode) {
        AuthFallbackMode.DEVICE_CREDENTIAL -> {
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock LockLens")
                .setSubtitle("Use fingerprint, face unlock, or your device screen lock")
                .setAllowedAuthenticators(DEVICE_CREDENTIAL_AUTHENTICATORS)
                .build()
        }
        AuthFallbackMode.APP_PIN -> {
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock LockLens")
                .setSubtitle("Use fingerprint or face unlock, or choose your LockLens PIN")
                .setAllowedAuthenticators(BIOMETRIC_STRONG)
                .setNegativeButtonText("Use LockLens PIN")
                .build()
        }
    }

    prompt.authenticate(promptInfo)
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? {
    return when (this) {
        is FragmentActivity -> this
        is ContextWrapper -> baseContext.findFragmentActivity()
        else -> null
    }
}
