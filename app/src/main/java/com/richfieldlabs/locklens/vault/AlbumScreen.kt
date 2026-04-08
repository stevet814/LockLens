package com.richfieldlabs.locklens.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.richfieldlabs.locklens.data.model.Photo
import com.richfieldlabs.locklens.ui.components.EmptyState
import com.richfieldlabs.locklens.ui.components.PhotoThumbnail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    onBack: () -> Unit,
    onOpenPhoto: (Long) -> Unit,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var photoToDelete by remember { mutableStateOf<Photo?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.albumName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.photos.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Folder,
                title = "Album is empty",
                body = "Photos assigned to this album will appear here.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
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
                        loadThumbnail = { viewModel.decryptThumbnail(photo) },
                        onClick = { onOpenPhoto(photo.id) },
                        onLongClick = { photoToDelete = photo },
                    )
                }
            }
        }
    }

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
}
