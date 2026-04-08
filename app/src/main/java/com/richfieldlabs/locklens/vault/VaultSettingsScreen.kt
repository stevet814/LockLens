package com.richfieldlabs.locklens.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.richfieldlabs.locklens.auth.AuthFallbackMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSettingsScreen(
    onBack: () -> Unit,
    onOpenDeviceSecuritySettings: () -> Unit,
    viewModel: VaultSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lock settings") },
                navigationIcon = {
                    androidx.compose.material3.TextButton(onClick = onBack) {
                        Text("Back")
                    }
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
            item {
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
                            text = "Unlock fallback",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Choose which secret LockLens uses if biometric unlock needs a fallback.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                FallbackModeOption(
                    title = "Phone PIN, pattern, or password",
                    description = "Uses Android device credential as the fallback. This is the native system flow.",
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
                        "Uses a vault-only PIN instead of your phone credential. You will create the PIN the next time the vault locks."
                    },
                    selected = uiState.fallbackMode == AuthFallbackMode.APP_PIN,
                    onClick = { viewModel.selectFallbackMode(AuthFallbackMode.APP_PIN) },
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = if (uiState.fallbackMode == AuthFallbackMode.APP_PIN) {
                                "LockLens PIN is required for any future decoy-PIN style flow."
                            } else {
                                "Device credential keeps unlock behavior aligned with the rest of Android."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        if (uiState.fallbackMode == AuthFallbackMode.DEVICE_CREDENTIAL && uiState.hasAppPin) {
                            Text(
                                text = "Your existing LockLens PIN stays stored so you can switch back later.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
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
