package com.richfieldlabs.locklens.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.richfieldlabs.locklens.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onBack: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel(),
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Secure camera") })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            EmptyState(
                icon = Icons.Default.CameraAlt,
                title = "Camera UI is next",
                body = "The secure save pipeline is scaffolded. The on-device CameraX preview and capture flow still needs to be wired in.",
            )
            Button(onClick = onBack) {
                Text("Back to vault")
            }
        }
    }
}
