package com.gamilo.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Precision Utility: sharp corners everywhere, 0-4px max — no Material soft-shadow rounding. */
object GamiloShapes {
    val Sharp = RoundedCornerShape(0.dp)
    val Slight = RoundedCornerShape(2.dp)
}

object GamiloDimens {
    val BorderWidth = 1.dp
    val ScreenPadding = 16.dp
    val TapTargetHeight = 56.dp
}
