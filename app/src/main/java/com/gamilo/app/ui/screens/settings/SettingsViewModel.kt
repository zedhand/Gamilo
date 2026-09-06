package com.gamilo.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamilo.app.backup.BackupManager
import com.gamilo.app.data.GamiloDatabase
import com.gamilo.app.data.model.Region
import com.gamilo.app.export.DataExportService
import com.gamilo.app.settings.GamiloSettings
import com.gamilo.app.settings.SettingsStore
import com.gamilo.app.ui.theme.GamiloThemeVariant
import java.io.InputStream
import java.io.OutputStream
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val backupManager: BackupManager,
    private val database: GamiloDatabase,
    private val dataExportService: DataExportService,
) : ViewModel() {

    val settings: Flow<GamiloSettings> = settingsStore.settings

    fun setTheme(variant: GamiloThemeVariant) = viewModelScope.launch { settingsStore.setTheme(variant) }
    fun setRegion(region: Region) = viewModelScope.launch { settingsStore.setRegion(region) }
    fun setManualFxRateToCad(rate: BigDecimal) = viewModelScope.launch { settingsStore.setManualFxRateToCad(rate) }
    fun setDefaultHourlyRate(rate: BigDecimal) = viewModelScope.launch { settingsStore.setDefaultHourlyRate(rate) }
    fun setDefaultMileageRatePerKm(rate: BigDecimal) = viewModelScope.launch { settingsStore.setDefaultMileageRatePerKm(rate) }
    fun setTaxRates(gstRate: BigDecimal, pstRate: BigDecimal) = viewModelScope.launch { settingsStore.setTaxRates(gstRate, pstRate) }

    /** Checkpoints the live WAL into the main file first so the exported single file is a complete, consistent snapshot. */
    fun exportBackup(outputStream: OutputStream, onComplete: (success: Boolean) -> Unit) = viewModelScope.launch {
        val success = runCatching {
            withContext(Dispatchers.IO) {
                database.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
                backupManager.exportTo(outputStream)
            }
        }.isSuccess
        onComplete(success)
    }

    /** Overwrites the on-disk database file. The caller must restart the app afterward — see SettingsScreen. */
    fun importBackup(inputStream: InputStream, onComplete: (success: Boolean) -> Unit) = viewModelScope.launch {
        val success = runCatching {
            withContext(Dispatchers.IO) {
                database.close()
                backupManager.importFrom(inputStream)
            }
        }.isSuccess
        onComplete(success)
    }

    /** Builds one combined CSV covering every entity (including soft-deleted rows) and writes it to [outputStream]. */
    fun exportCsv(outputStream: OutputStream, onComplete: (success: Boolean) -> Unit) = viewModelScope.launch {
        val success = runCatching {
            val csv = dataExportService.buildCombinedCsv()
            withContext(Dispatchers.IO) { outputStream.write(csv.toByteArray()) }
        }.isSuccess
        onComplete(success)
    }

    /**
     * The actual wipe — callers (SettingsScreen's Danger Zone) must have already gated this
     * behind a fresh biometric re-auth AND a typed "DELETE" confirmation. There is no further
     * confirmation here. The caller must restart the app afterward — see SettingsScreen.
     */
    fun factoryReset(onComplete: (success: Boolean) -> Unit) = viewModelScope.launch {
        val success = runCatching {
            withContext(Dispatchers.IO) {
                database.close()
                backupManager.wipeAllData()
            }
            settingsStore.clearAll()
        }.isSuccess
        onComplete(success)
    }
}
