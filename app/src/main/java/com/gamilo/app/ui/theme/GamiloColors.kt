package com.gamilo.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

private data class GamiloPalette(
    val background: Color,
    val surface: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    /** Equal to [accent] for single-accent schemes; a distinct second hue for the two-tone ones (High-Vis, Safety Light). */
    val accentSecondary: Color,
)

// Every hex named in the master design spec is used verbatim below. Surface/border/
// textSecondary weren't individually specified for every scheme — those are derived to
// keep the same hard-edged structural discipline (sharp corners, 1px borders, no soft
// Material shadows) rather than invented arbitrarily.

private val ObsidianAmber = GamiloPalette(
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    border = Color(0xFF334155),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF94A3B8),
    accent = Color(0xFFF59E0B),
    accentSecondary = Color(0xFFF59E0B),
)

private val BlueprintCyan = GamiloPalette(
    background = Color(0xFF0A192F),
    surface = Color(0xFF11233F),
    border = Color(0xFF1E3A5F),
    textPrimary = Color(0xFFE6F1FF),
    textSecondary = Color(0xFF7A93B8),
    accent = Color(0xFF00E5FF),
    accentSecondary = Color(0xFF00E5FF),
)

private val TerminalGreen = GamiloPalette(
    background = Color(0xFF000000),
    surface = Color(0xFF0D0D0D),
    border = Color(0xFF2A2A2A),
    textPrimary = Color(0xFFE5FFE5),
    textSecondary = Color(0xFF6B8F6B),
    accent = Color(0xFF39FF14),
    accentSecondary = Color(0xFF39FF14),
)

private val HighVisConstruction = GamiloPalette(
    background = Color(0xFF1C1C1E),
    surface = Color(0xFF262628),
    border = Color(0xFF48484A),
    textPrimary = Color(0xFFF5F5F5),
    textSecondary = Color(0xFF9A9A9C),
    accent = Color(0xFFFFEA00),
    accentSecondary = Color(0xFFFF6D00),
)

private val CrimsonSteel = GamiloPalette(
    background = Color(0xFF212529),
    surface = Color(0xFF2B2F33),
    border = Color(0xFF495057),
    textPrimary = Color(0xFFF8F9FA),
    textSecondary = Color(0xFFADB5BD),
    accent = Color(0xFFD90429),
    accentSecondary = Color(0xFFD90429),
)

private val ArcticSlate = GamiloPalette(
    background = Color(0xFF1B263B),
    surface = Color(0xFF22304A),
    border = Color(0xFF415A77),
    textPrimary = Color(0xFFE0FBFC),
    textSecondary = Color(0xFF90A8C4),
    accent = Color(0xFF48CAE4),
    accentSecondary = Color(0xFF48CAE4),
)

private val DraftingTable = GamiloPalette(
    background = Color(0xFFF5F1E8),
    surface = Color(0xFFEDE7D9),
    border = Color(0xFF6B6B63),
    textPrimary = Color(0xFF1A1F2E),
    textSecondary = Color(0xFF4B5165),
    accent = Color(0xFF1E3A5F),
    accentSecondary = Color(0xFF1E3A5F),
)

private val BlueprintReverse = GamiloPalette(
    background = Color(0xFFE8ECEF),
    surface = Color(0xFFDCE1E5),
    border = Color(0xFF94A3B8),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    accent = Color(0xFF0891B2),
    accentSecondary = Color(0xFF0891B2),
)

private val SafetyLight = GamiloPalette(
    background = Color(0xFFF4F4F5),
    surface = Color(0xFFE9E9EB),
    border = Color(0xFF52525B),
    textPrimary = Color(0xFF000000),
    textSecondary = Color(0xFF52525B),
    accent = Color(0xFFEA580C),
    accentSecondary = Color(0xFFCA8A04),
)

/** The accent color a swatch preview should show for [variant], without switching the live theme. */
fun accentPreviewColor(variant: GamiloThemeVariant): Color = paletteFor(variant).accent

private fun paletteFor(variant: GamiloThemeVariant): GamiloPalette = when (variant) {
    GamiloThemeVariant.OBSIDIAN_AMBER -> ObsidianAmber
    GamiloThemeVariant.BLUEPRINT_CYAN -> BlueprintCyan
    GamiloThemeVariant.TERMINAL_GREEN -> TerminalGreen
    GamiloThemeVariant.HIGH_VIS_CONSTRUCTION -> HighVisConstruction
    GamiloThemeVariant.CRIMSON_STEEL -> CrimsonSteel
    GamiloThemeVariant.ARCTIC_SLATE -> ArcticSlate
    GamiloThemeVariant.DRAFTING_TABLE -> DraftingTable
    GamiloThemeVariant.BLUEPRINT_REVERSE -> BlueprintReverse
    GamiloThemeVariant.SAFETY_LIGHT -> SafetyLight
}

/**
 * Precision Utility palette, live-switchable across all 9 schemes. Every field is a Compose
 * [androidx.compose.runtime.State] (`by mutableStateOf`), not a plain `val`, so [applyTheme]
 * instantly recomposes every screen the moment Settings changes theme — call sites never
 * need to change when a Stage adds screens.
 */
object GamiloColors {
    var current: GamiloThemeVariant = GamiloThemeVariant.OBSIDIAN_AMBER
        private set

    var Background by mutableStateOf(ObsidianAmber.background)
        private set
    var Surface by mutableStateOf(ObsidianAmber.surface)
        private set
    var Border by mutableStateOf(ObsidianAmber.border)
        private set
    var TextPrimary by mutableStateOf(ObsidianAmber.textPrimary)
        private set
    var TextSecondary by mutableStateOf(ObsidianAmber.textSecondary)
        private set
    var Accent by mutableStateOf(ObsidianAmber.accent)
        private set
    var AccentSecondary by mutableStateOf(ObsidianAmber.accentSecondary)
        private set

    /**
     * Deliberately theme-invariant, unlike every other token above: a destructive action (the
     * Settings Danger Zone) must always read as "red" regardless of which accent a scheme
     * picked — Terminal Green's accent is green, Obsidian Amber's is amber, neither reads as a
     * warning. Legible against every one of the 9 palette backgrounds (dark and light alike).
     */
    val Danger = Color(0xFFDC2626)

    fun applyTheme(variant: GamiloThemeVariant) {
        if (variant == current) return
        val palette = paletteFor(variant)
        current = variant
        Background = palette.background
        Surface = palette.surface
        Border = palette.border
        TextPrimary = palette.textPrimary
        TextSecondary = palette.textSecondary
        Accent = palette.accent
        AccentSecondary = palette.accentSecondary
    }
}
