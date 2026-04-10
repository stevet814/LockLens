package com.richfieldlabs.locklens.billing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Renders [content] as-is when Pro is unlocked.
 * When not Pro, an invisible overlay intercepts all taps and calls [onUpgradeClick].
 */
@Composable
fun ProGate(
    isProUnlocked: Boolean,
    onUpgradeClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box {
        content()
        if (!isProUnlocked) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { onUpgradeClick() },
            )
        }
    }
}
