package com.gamilo.app.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamilo.app.ui.theme.GamiloColors
import com.gamilo.app.ui.theme.GamiloDimens

/**
 * Precision Utility bottom nav: a hard 1px top border instead of a Material elevation
 * shadow, sharp rectangular tap targets sized for gloved-hand use on a job site.
 */
@Composable
fun GamiloBottomBar(current: BottomDestination, onSelect: (BottomDestination) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GamiloColors.Surface)
            .border(GamiloDimens.BorderWidth, GamiloColors.Border),
    ) {
        BottomDestination.entries.forEach { destination ->
            val selected = destination == current
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(GamiloDimens.TapTargetHeight)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(destination) },
                    )
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = destination.label.uppercase(),
                    color = if (selected) GamiloColors.Accent else GamiloColors.TextSecondary,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp,
                )
            }
        }
    }
}
