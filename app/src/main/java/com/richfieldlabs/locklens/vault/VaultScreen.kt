package com.richfieldlabs.locklens.vault

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.richfieldlabs.locklens.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.richfieldlabs.locklens.auth.DecoyVault
import com.richfieldlabs.locklens.billing.ProGate
import com.richfieldlabs.locklens.billing.ProUpgradeSheet
import com.richfieldlabs.locklens.data.model.Photo
import com.richfieldlabs.locklens.ui.components.EmptyState
import com.richfieldlabs.locklens.ui.components.PhotoThumbnail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    decoyMode: Boolean,
    onOpenCamera: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPhoto: (Long) -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: VaultViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var photoToDelete by remember { mutableStateOf<Photo?>(null) }
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }
    var showUpgradeSheet by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) viewModel.importPhotos(uris)
    }

    LaunchedEffect(uiState.importError) {
        uiState.importError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.consumeImportError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
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
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "LockLens",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                actions = {
                    if (!decoyMode) {
                        if (uiState.isImporting) {
                            CircularProgressIndicator(modifier = Modifier.padding(12.dp))
                        } else {
                            IconButton(onClick = { importLauncher.launch(arrayOf("image/*", "video/*")) }) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Import from gallery",
                                )
                            }
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (!decoyMode) {
                FloatingActionButton(onClick = onOpenCamera) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Open secure camera")
                }
            }
        },
    ) { innerPadding ->
        when {
            decoyMode -> {
                DecoyVault(modifier = Modifier.padding(innerPadding))
            }
            uiState.photos.isEmpty() -> {
                EmptyState(
                    icon = Icons.Default.CameraAlt,
                    title = "Your vault is empty",
                    body = "Tap the camera button to take a private photo, or use the import button to add photos from your gallery.",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    // Albums row — always shown; overlay intercepts taps when not Pro
                    ProGate(
                        isProUnlocked = uiState.isProUnlocked,
                        onUpgradeClick = { showUpgradeSheet = true },
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(uiState.albums, key = { it.id }) { album ->
                                Card(onClick = { onOpenAlbum(album.id) }) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(album.name, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                            item {
                                Card(
                                    onClick = { showCreateAlbumDialog = true },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    ),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = null)
                                        Text("New album", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(uiState.photos, key = { it.id }) { photo ->
                            PhotoThumbnail(
                                photo = photo,
                                loadThumbnail = { viewModel.decryptThumbnail(photo) },
                                onClick = { onOpenPhoto(photo.id) },
                                onLongClick = { photoToDelete = photo },
                            )
                        }
                    }
                }
            }
        }
    }

    // Long-press delete confirmation
    photoToDelete?.let { photo ->
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            title = { Text("Delete photo?") },
            text = { Text("This permanently deletes the encrypted photo. It cannot be recovered.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePhoto(photo)
                        photoToDelete = null
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { photoToDelete = null }) { Text("Cancel") }
            },
        )
    }

    // Create album dialog
    if (showCreateAlbumDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateAlbumDialog = false
                newAlbumName = ""
            },
            title = { Text("New album") },
            text = {
                OutlinedTextField(
                    value = newAlbumName,
                    onValueChange = { newAlbumName = it },
                    label = { Text("Album name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newAlbumName.isNotBlank()) viewModel.createAlbum(newAlbumName)
                        showCreateAlbumDialog = false
                        newAlbumName = ""
                    },
                    enabled = newAlbumName.isNotBlank(),
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateAlbumDialog = false
                    newAlbumName = ""
                }) { Text("Cancel") }
            },
        )
    }

    // Free-tier limit banner
    if (!decoyMode && !uiState.isProUnlocked && uiState.photos.size >= 90) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                ),
            ) {
                Text(
                    text = "${uiState.photos.size}/100 photos — upgrade to Pro for unlimited.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }

    // Pro upgrade sheet
    if (showUpgradeSheet) {
        ProUpgradeSheet(
            onDismiss = { showUpgradeSheet = false },
            onPurchaseClick = { activity ->
                viewModel.launchPurchaseFlow(activity)
                showUpgradeSheet = false
            },
        )
    }
}
