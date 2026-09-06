package com.gamilo.app.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.gamilo.app.data.model.Region
import com.gamilo.app.ui.theme.GamiloThemeVariant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.math.BigDecimal
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@SmallTest
class SettingsStoreTest {

    private lateinit var file: File
    private lateinit var store: SettingsStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        file = File(context.filesDir, "test_settings_${UUID.randomUUID()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { file })
        store = SettingsStore(dataStore)
    }

    @After
    fun tearDown() {
        file.delete()
    }

    @Test
    fun defaultSettings_areReturnedWhenNothingStoredYet() = runTest {
        val settings = store.settings.first()
        assertEquals(GamiloSettings.DEFAULT.region, settings.region)
        assertEquals(GamiloSettings.DEFAULT.themeVariant, settings.themeVariant)
    }

    @Test
    fun setTheme_persistsAndRoundTrips() = runTest {
        store.setTheme(GamiloThemeVariant.TERMINAL_GREEN)
        assertEquals(GamiloThemeVariant.TERMINAL_GREEN, store.settings.first().themeVariant)
    }

    @Test
    fun setRegion_reseedsBaseCurrencyCode() = runTest {
        store.setRegion(Region.UNITED_STATES)
        val settings = store.settings.first()
        assertEquals(Region.UNITED_STATES, settings.region)
        assertEquals("USD", settings.baseCurrencyCode)
    }

    @Test
    fun setManualFxRateToCad_rejectsNonPositiveRate() = runTest {
        try {
            store.setManualFxRateToCad(BigDecimal.ZERO)
            throw AssertionError("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun setDefaultMileageRatePerKm_persists() = runTest {
        store.setDefaultMileageRatePerKm(BigDecimal("0.72"))
        assertEquals(BigDecimal("0.72"), store.settings.first().defaultMileageRatePerKm)
    }

    @Test
    fun setTaxRates_persistsBothRates() = runTest {
        store.setTaxRates(BigDecimal("0.05"), BigDecimal("0.07"))
        val settings = store.settings.first()
        assertEquals(BigDecimal("0.05"), settings.defaultGstRate)
        assertEquals(BigDecimal("0.07"), settings.defaultPstRate)
    }

    @Test
    fun clearAll_resetsToDefaults() = runTest {
        store.setTheme(GamiloThemeVariant.ARCTIC_SLATE)
        store.setRegion(Region.UNITED_STATES)
        store.clearAll()

        val settings = store.settings.first()
        assertEquals(GamiloSettings.DEFAULT.themeVariant, settings.themeVariant)
        assertEquals(GamiloSettings.DEFAULT.region, settings.region)
    }
}
