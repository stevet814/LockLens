package com.richfieldlabs.locklens.vault

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.richfieldlabs.locklens.auth.AuthFallbackMode
import com.richfieldlabs.locklens.auth.LockTimeout
import com.richfieldlabs.locklens.billing.ProGate
import com.richfieldlabs.locklens.billing.ProUpgradeSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSettingsScreen(
    onBack: () -> Unit,
    onOpenDeviceSecuritySettings: () -> Unit,
    onOpenIntruderLog: () -> Unit,
    viewModel: VaultSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity

    var showUpgradeSheet by remember { mutableStateOf(false) }

    var showDecoyPinDialog by remember { mutableStateOf(false) }
    var decoyStep by remember { mutableIntStateOf(1) }
    var decoyFirst by remember { mutableStateOf("") }
    var decoyInput by remember { mutableStateOf("") }
    var decoyError by remember { mutableStateOf<String?>(null) }

    fun resetDecoyDialog() {
        showDecoyPinDialog = false
        decoyStep = 1
        decoyFirst = ""
        decoyInput = ""
        decoyError = null
        viewModel.consumeDecoyPinResult()
    }

    var showChangePinDialog by remember { mutableStateOf(false) }
    var changePinStep by remember { mutableIntStateOf(1) }
    var changePinCurrent by remember { mutableStateOf("") }
    var changePinNew by remember { mutableStateOf("") }
    var changePinInput by remember { mutableStateOf("") }

    fun resetChangePinDialog() {
        showChangePinDialog = false
        changePinStep = 1
        changePinCurrent = ""
        changePinNew = ""
        changePinInput = ""
        viewModel.consumeChangePinResult()
    }

    LaunchedEffect(uiState.changePinSuccess) {
        if (uiState.changePinSuccess) {
            resetChangePinDialog()
        }
    }

    LaunchedEffect(uiState.decoyPinError) {
        uiState.decoyPinError?.let { decoyError = it }
    }

    LaunchedEffect(uiState.decoyPinSuccess) {
        if (uiState.decoyPinSuccess) {
            resetDecoyDialog()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (uiState.isProUnlocked) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "Pro unlocked",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = "You have full access to all LockLens features.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Auto-lock") {
                    Text(
                        text = "Lock vault when LockLens leaves the foreground.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LockTimeout.entries.forEach { timeout ->
                        RadioRow(
                            label = timeout.displayName,
                            selected = uiState.lockTimeout == timeout,
                            onClick = { viewModel.setLockTimeout(timeout) },
                        )
                    }
                }
            }

            if (uiState.fallbackMode == AuthFallbackMode.APP_PIN && uiState.hasAppPin) {
                item {
                    SectionCard(title = "LockLens PIN") {
                        OutlinedButton(
                            onClick = { showChangePinDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Change PIN")
                        }
                    }
                }
            }

            item {
                ProGate(
                    isProUnlocked = uiState.isProUnlocked,
                    onUpgradeClick = { showUpgradeSheet = true },
                ) {
                    SectionCard(title = "Decoy PIN") {
                        Text(
                            text = if (uiState.hasDecoyPin) {
                                "A decoy PIN is set. Entering it opens the decoy vault instead of your real vault."
                            } else {
                                "Set a second PIN that opens the decoy vault instead of your real vault."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = { showDecoyPinDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (uiState.hasDecoyPin) "Change decoy PIN" else "Set decoy PIN")
                        }
                    }
                }
            }

            item {
                ProGate(
                    isProUnlocked = uiState.isProUnlocked,
                    onUpgradeClick = { showUpgradeSheet = true },
                ) {
                    Card(
                        onClick = onOpenIntruderLog,
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Intrusion log",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "View failed unlock attempts and break-in selfies.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Unlock fallback") {
                    Text(
                        text = "Choose which secret LockLens uses if biometric needs a fallback.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                FallbackModeOption(
                    title = "Phone PIN, pattern, or password",
                    description = "Uses Android device credential. Native system flow.",
                    selected = uiState.fallbackMode == AuthFallbackMode.DEVICE_CREDENTIAL,
                    onClick = { viewModel.selectFallbackMode(AuthFallbackMode.DEVICE_CREDENTIAL) },
                )
            }

            item {
                FallbackModeOption(
                    title = "LockLens PIN",
                    description = if (uiState.hasAppPin) {
                        "Uses a vault-only PIN instead of your phone credential."
                    } else {
                        "Uses a vault-only PIN. You'll create it the next time the vault locks."
                    },
                    selected = uiState.fallbackMode == AuthFallbackMode.APP_PIN,
                    onClick = { viewModel.selectFallbackMode(AuthFallbackMode.APP_PIN) },
                )
            }

            item {
                Button(
                    onClick = onOpenDeviceSecuritySettings,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Open device security settings")
                }
            }
        }
    }

    if (showUpgradeSheet) {
        ProUpgradeSheet(
            onDismiss = { showUpgradeSheet = false },
            onPurchaseClick = { act ->
                viewModel.launchPurchaseFlow(act)
                showUpgradeSheet = false
            },
        )
    }

    if (showDecoyPinDialog) {
        PinEntryDialog(
            title = if (decoyStep == 1) "Set decoy PIN" else "Confirm decoy PIN",
            hint = if (decoyStep == 1) {
                "Enter a PIN for the decoy vault. It must be different from your LockLens PIN."
            } else {
                "Enter the same PIN again."
            },
            label = "Decoy PIN",
            value = decoyInput,
            onValueChange = {
                if (it.all(Char::isDigit) && it.length <= 6) {
                    decoyInput = it
                    decoyError = null
                }
            },
            error = decoyError,
            confirmLabel = if (decoyStep == 1) "Next" else "Save",
            onConfirm = {
                if (decoyStep == 1) {
                    decoyFirst = decoyInput
                    decoyInput = ""
                    decoyStep = 2
                } else if (decoyInput == decoyFirst) {
                    viewModel.setDecoyPin(decoyInput)
                } else {
                    decoyError = "PINs do not match."
                    decoyInput = ""
                }
            },
            onDismiss = { resetDecoyDialog() },
        )
    }

    if (showChangePinDialog) {
        val (title, hint, label) = when (changePinStep) {
            1 -> Triple("Change PIN", "Enter your current PIN to continue.", "Current PIN")
            2 -> Triple("Change PIN", "Enter your new PIN.", "New PIN")
            else -> Triple("Change PIN", "Confirm your new PIN.", "Confirm new PIN")
        }
        PinEntryDialog(
            title = title,
            hint = hint,
            label = label,
            value = changePinInput,
            onValueChange = {
                if (it.all(Char::isDigit) && it.length <= 6) {
                    changePinInput = it
                }
            },
            error = if (changePinStep == 1) uiState.changePinError else null,
            confirmLabel = if (changePinStep < 3) "Next" else "Save",
            onConfirm = {
                when (changePinStep) {
                    1 -> {
                        changePinCurrent = changePinInput
                        changePinInput = ""
                        changePinStep = 2
                    }
                    2 -> {
                        changePinNew = changePinInput
                        changePinInput = ""
                        changePinStep = 3
                    }
                    else -> {
                        if (changePinInput == changePinNew) {
                            viewModel.changeRealPin(changePinCurrent, changePinNew)
                        } else {
                            changePinInput = ""
                            changePinStep = 2
                        }
                    }
                }
            },
            onDismiss = { resetChangePinDialog() },
        )
    }
}

@Composable
private fun PinEntryDialog(
    title: String,
    hint: String,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(hint, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = { Text(label) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = value.length >= 4, onClick = onConfirm) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun RadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FallbackModeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
