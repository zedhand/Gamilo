package com.gamilo.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val GamiloTypography = Typography(
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
)

/** Numeric values (currency, mileage, hours) must always render in this to keep table columns aligned. */
val MonospaceNumeric = FontFamily.Monospace

@Composable
fun GamiloTheme(content: @Composable () -> Unit) {
    val colorScheme = if (GamiloColors.current.isDark) {
        darkColorScheme(
            background = GamiloColors.Background,
            surface = GamiloColors.Surface,
            primary = GamiloColors.Accent,
            secondary = GamiloColors.AccentSecondary,
            onBackground = GamiloColors.TextPrimary,
            onSurface = GamiloColors.TextPrimary,
            outline = GamiloColors.Border,
        )
    } else {
        lightColorScheme(
            background = GamiloColors.Background,
            surface = GamiloColors.Surface,
            primary = GamiloColors.Accent,
            secondary = GamiloColors.AccentSecondary,
            onBackground = GamiloColors.TextPrimary,
            onSurface = GamiloColors.TextPrimary,
            outline = GamiloColors.Border,
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = GamiloTypography,
        content = content,
    )
}
