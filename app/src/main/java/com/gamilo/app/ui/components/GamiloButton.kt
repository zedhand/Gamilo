package com.gamilo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamilo.app.ui.theme.GamiloColors

/**
 * The large tactile primary-action button (Home's Start/End Shift, per-tab "+ ADD" actions).
 * A 2px accent border + bold accent label on Surface, not a solid accent fill — a fill would
 * need a different text color picked per theme for contrast (bright yellow/cyan accents vs.
 * dark ones); border+label reuses the same accent-as-foreground pattern as the Settings
 * theme rows, so it's guaranteed legible across all 9 schemes without per-theme logic.
 */
@Composable
fun GamiloButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tall: Boolean = false,
    danger: Boolean = false,
    enabled: Boolean = true,
) {
    val color = when {
        !enabled -> GamiloColors.TextSecondary
        danger -> GamiloColors.Danger
        else -> GamiloColors.Accent
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (tall) 72.dp else 56.dp)
            .background(GamiloColors.Surface)
            .border(2.dp, color)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase(),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = if (tall) 18.sp else 15.sp,
        )
    }
}
