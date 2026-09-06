package com.gamilo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gamilo.app.ui.theme.GamiloColors
import com.gamilo.app.ui.theme.GamiloDimens

/** Shared bordered row container for every tab's list — sharp corners, hard 1px border. */
@Composable
fun ListRowCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(GamiloColors.Surface)
            .border(GamiloDimens.BorderWidth, GamiloColors.Border)
            .padding(12.dp),
    ) {
        content()
    }
}
