package com.gamilo.app.settings

import com.gamilo.app.data.model.Region
import com.gamilo.app.ui.theme.GamiloThemeVariant
import java.math.BigDecimal

/**
 * Gamilo is air-gapped and fetches no live FX or tax rates — every value here is a manual
 * default that a new Hours/Materials/Mileage/Shipping entry starts from, editable per entry
 * before it's saved. Changing a setting only affects *future* entries: it must never rewrite
 * currencyCode/fxRateApplied/gstRateApplied/pstRateApplied/mileageRateApplied already frozen
 * on past rows (see the repositories in data/repo/, which freeze these at write time).
 *
 * The BC GST+PST tax jurisdiction itself is fixed regardless of [region] — [region] only
 * seeds [baseCurrencyCode]/[manualFxRateToCad] for a shop that occasionally bills in USD.
 */
data class GamiloSettings(
    val region: Region,
    val baseCurrencyCode: String,
    val manualFxRateToCad: BigDecimal,
    val defaultHourlyRate: BigDecimal,
    val defaultMileageRatePerKm: BigDecimal,
    val defaultGstRate: BigDecimal,
    val defaultPstRate: BigDecimal,
    val themeVariant: GamiloThemeVariant = GamiloThemeVariant.OBSIDIAN_AMBER,
) {
    /** The fx rate to freeze onto a new row priced in [baseCurrencyCode]: always exactly 1 for CAD. */
    val currentFxRateApplied: BigDecimal get() = if (baseCurrencyCode == "CAD") BigDecimal.ONE else manualFxRateToCad

    companion object {
        val DEFAULT = GamiloSettings(
            region = Region.CANADA,
            baseCurrencyCode = "CAD",
            manualFxRateToCad = BigDecimal.ONE,
            defaultHourlyRate = BigDecimal("45.00"),
            // CRA business mileage rate, $/km — illustrative default, editable in Settings.
            defaultMileageRatePerKm = BigDecimal("0.70"),
            // Current BC rates: 5% federal GST + 7% provincial PST.
            defaultGstRate = BigDecimal("0.05"),
            defaultPstRate = BigDecimal("0.07"),
        )
    }
}
