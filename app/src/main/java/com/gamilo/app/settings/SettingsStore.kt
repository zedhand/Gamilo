package com.gamilo.app.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gamilo.app.data.model.Region
import com.gamilo.app.ui.theme.GamiloThemeVariant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

class SettingsStore(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val REGION = stringPreferencesKey("region")
        val BASE_CURRENCY = stringPreferencesKey("base_currency_code")
        val MANUAL_FX_RATE = stringPreferencesKey("manual_fx_rate_to_cad")
        val DEFAULT_HOURLY_RATE = stringPreferencesKey("default_hourly_rate")
        val MILEAGE_RATE = stringPreferencesKey("default_mileage_rate_per_km")
        val GST_RATE = stringPreferencesKey("default_gst_rate")
        val PST_RATE = stringPreferencesKey("default_pst_rate")
        val THEME = stringPreferencesKey("selected_theme")
    }

    val settings: Flow<GamiloSettings> = dataStore.data.map { prefs ->
        val default = GamiloSettings.DEFAULT
        GamiloSettings(
            region = prefs[Keys.REGION]?.let { runCatching { Region.valueOf(it) }.getOrNull() } ?: default.region,
            baseCurrencyCode = prefs[Keys.BASE_CURRENCY] ?: default.baseCurrencyCode,
            manualFxRateToCad = prefs[Keys.MANUAL_FX_RATE]?.let { runCatching { BigDecimal(it) }.getOrNull() }
                ?: default.manualFxRateToCad,
            defaultHourlyRate = prefs[Keys.DEFAULT_HOURLY_RATE]?.let { runCatching { BigDecimal(it) }.getOrNull() }
                ?: default.defaultHourlyRate,
            defaultMileageRatePerKm = prefs[Keys.MILEAGE_RATE]?.let { runCatching { BigDecimal(it) }.getOrNull() }
                ?: default.defaultMileageRatePerKm,
            defaultGstRate = prefs[Keys.GST_RATE]?.let { runCatching { BigDecimal(it) }.getOrNull() }
                ?: default.defaultGstRate,
            defaultPstRate = prefs[Keys.PST_RATE]?.let { runCatching { BigDecimal(it) }.getOrNull() }
                ?: default.defaultPstRate,
            themeVariant = GamiloThemeVariant.fromStorageKey(prefs[Keys.THEME]),
        )
    }

    /** Switching region reseeds the base-currency default for NEW entries only; it never touches existing rows. */
    suspend fun setRegion(region: Region) {
        dataStore.edit { prefs ->
            prefs[Keys.REGION] = region.name
            prefs[Keys.BASE_CURRENCY] = if (region == Region.CANADA) "CAD" else "USD"
        }
    }

    suspend fun setManualFxRateToCad(rate: BigDecimal) {
        require(rate > BigDecimal.ZERO) { "manualFxRateToCad must be positive, was $rate" }
        dataStore.edit { it[Keys.MANUAL_FX_RATE] = rate.toPlainString() }
    }

    suspend fun setDefaultHourlyRate(rate: BigDecimal) {
        require(rate >= BigDecimal.ZERO) { "defaultHourlyRate must not be negative, was $rate" }
        dataStore.edit { it[Keys.DEFAULT_HOURLY_RATE] = rate.toPlainString() }
    }

    suspend fun setDefaultMileageRatePerKm(rate: BigDecimal) {
        require(rate >= BigDecimal.ZERO) { "defaultMileageRatePerKm must not be negative, was $rate" }
        dataStore.edit { it[Keys.MILEAGE_RATE] = rate.toPlainString() }
    }

    suspend fun setTaxRates(gstRate: BigDecimal, pstRate: BigDecimal) {
        dataStore.edit { prefs ->
            prefs[Keys.GST_RATE] = gstRate.toPlainString()
            prefs[Keys.PST_RATE] = pstRate.toPlainString()
        }
    }

    suspend fun setTheme(variant: GamiloThemeVariant) {
        dataStore.edit { it[Keys.THEME] = variant.storageKey }
    }

    /** Part of factory reset (Stage 5's Danger Zone) — wipes region/rates/theme back to defaults. */
    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
