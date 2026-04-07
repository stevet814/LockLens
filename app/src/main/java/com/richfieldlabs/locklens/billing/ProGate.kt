package com.richfieldlabs.locklens.billing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProGate(
    isProUnlocked: Boolean,
    featureName: String,
    onUpgradeClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (isProUnlocked) {
        content()
    } else {
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "$featureName is a Pro feature.",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text("Upgrade once to unlock the full privacy toolkit.")
                Button(onClick = onUpgradeClick) {
                    Text("Upgrade to Pro")
                }
            }
        }
    }
}
