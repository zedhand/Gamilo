package com.gamilo.app.ui.theme

/** The 9 Precision Utility color schemes (6 dark, 3 light), persisted via SettingsStore. */
enum class GamiloThemeVariant(val storageKey: String, val label: String, val isDark: Boolean) {
    OBSIDIAN_AMBER("OBSIDIAN_AMBER", "Obsidian & Amber", isDark = true),
    BLUEPRINT_CYAN("BLUEPRINT_CYAN", "Blueprint & Cyan", isDark = true),
    TERMINAL_GREEN("TERMINAL_GREEN", "Terminal Green", isDark = true),
    HIGH_VIS_CONSTRUCTION("HIGH_VIS_CONSTRUCTION", "High-Vis Construction", isDark = true),
    CRIMSON_STEEL("CRIMSON_STEEL", "Crimson & Steel", isDark = true),
    ARCTIC_SLATE("ARCTIC_SLATE", "Arctic Slate", isDark = true),
    DRAFTING_TABLE("DRAFTING_TABLE", "Drafting Table", isDark = false),
    BLUEPRINT_REVERSE("BLUEPRINT_REVERSE", "Blueprint Reverse", isDark = false),
    SAFETY_LIGHT("SAFETY_LIGHT", "Safety Light", isDark = false);

    companion object {
        fun fromStorageKey(key: String?): GamiloThemeVariant =
            entries.firstOrNull { it.storageKey == key } ?: OBSIDIAN_AMBER
    }
}
