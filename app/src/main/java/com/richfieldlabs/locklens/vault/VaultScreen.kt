package com.richfieldlabs.locklens.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.richfieldlabs.locklens.auth.DecoyVault
import com.richfieldlabs.locklens.ui.components.EmptyState
import com.richfieldlabs.locklens.ui.components.PhotoThumbnail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    decoyMode: Boolean,
    onOpenCamera: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPhoto: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: VaultViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (decoyMode) "Vault" else "Encrypted vault") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Vault",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (!decoyMode) {
                FloatingActionButton(onClick = onOpenCamera) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Open secure camera",
                    )
                }
            }
        },
    ) { innerPadding ->
        when {
            decoyMode -> {
                DecoyVault(
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.padding(innerPadding),
                )
            }
            uiState.photos.isEmpty() -> {
                EmptyState(
                    icon = Icons.Default.Shield,
                    title = "Vault is ready",
                    body = "Your encrypted gallery is set up. The next pass will wire secure capture into this grid.",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.photos, key = { it.id }) { photo ->
                        PhotoThumbnail(
                            photo = photo,
                            onClick = { onOpenPhoto(photo.id) },
                        )
                    }
                }
            }
        }
    }
}
