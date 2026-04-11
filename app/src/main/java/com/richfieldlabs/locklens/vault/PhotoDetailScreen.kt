package com.richfieldlabs.locklens.vault

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.richfieldlabs.locklens.billing.ProFeatureLockedState
import com.richfieldlabs.locklens.billing.ProUpgradeSheet

@Composable
fun PhotoDetailScreen(
    onBack: () -> Unit,
    viewModel: PhotoDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showUpgradeSheet by remember { mutableStateOf(false) }
    fun openUpgradeSheet() {
        showUpgradeSheet = true
    }
    fun dismissUpgradeSheet() {
        showUpgradeSheet = false
    }
    fun openDeleteDialog() {
        showDeleteDialog = true
    }
    fun dismissDeleteDialog() {
        showDeleteDialog = false
    }

    // Share launcher — deletes temp file after intent is handled
    val shareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onShareLaunched()
    }

    // When a share URI is ready, fire the intent
    LaunchedEffect(uiState.shareUri) {
        val uri = uiState.shareUri ?: return@LaunchedEffect
        val mimeType = uiState.photo?.mimeType ?: "*/*"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        shareLauncher.launch(Intent.createChooser(intent, "Share via"))
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset = if (scale <= 1f) Offset.Zero else offset + panChange
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                )
            }

            uiState.bitmap != null -> {
                Image(
                    bitmap = uiState.bitmap!!,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y,
                        )
                        .transformable(state = transformableState),
                )
            }

            uiState.videoUri != null -> {
                val player = remember {
                    ExoPlayer.Builder(context).build().also { player ->
                        player.setMediaItem(MediaItem.fromUri(uiState.videoUri!!))
                        player.prepare()
                        player.playWhenReady = true
                    }
                }
                DisposableEffect(uiState.videoUri) {
                    onDispose { player.release() }
                }
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            uiState.isVideoLocked -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ProFeatureLockedState(
                        title = "Video vault is a Pro feature",
                        body = "Upgrade to play encrypted videos and keep every media type together in LockLens.",
                        onUpgradeClick = ::openUpgradeSheet,
                    )
                }
            }

            uiState.error != null -> {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 4.dp, end = 4.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }

            Row {
                if (uiState.photo != null) {
                    IconButton(
                        onClick = {
                            if (uiState.isProUnlocked) {
                                viewModel.prepareShare()
                            } else {
                                openUpgradeSheet()
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White,
                        )
                    }
                }
                if (uiState.photo != null) {
                    IconButton(onClick = ::openDeleteDialog) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }

    if (showUpgradeSheet) {
        ProUpgradeSheet(
            onDismiss = ::dismissUpgradeSheet,
            onPurchaseClick = { activity ->
                viewModel.launchPurchaseFlow(activity)
                dismissUpgradeSheet()
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = ::dismissDeleteDialog,
            title = { Text("Delete photo?") },
            text = { Text("This permanently deletes the encrypted photo. It cannot be recovered.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        dismissDeleteDialog()
                        viewModel.deletePhoto(onDeleted = onBack)
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = ::dismissDeleteDialog) {
                    Text("Cancel")
                }
            },
        )
    }
}
