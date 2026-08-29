package com.gamilo.app.ui

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gamilo.app.data.GamiloDatabase
import com.gamilo.app.settings.SettingsStore
import org.junit.rules.ExternalResource
import java.io.File
import java.util.UUID

/** In-memory Room DB + a throwaway file-backed DataStore, for screen-level Compose UI tests. */
class ScreenTestRule : ExternalResource() {
    lateinit var database: GamiloDatabase
        private set
    lateinit var settingsStore: SettingsStore
        private set
    private lateinit var settingsFile: File

    override fun before() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, GamiloDatabase::class.java).allowMainThreadQueries().build()
        settingsFile = File(context.filesDir, "ui_test_settings_${UUID.randomUUID()}.preferences_pb")
        settingsStore = SettingsStore(PreferenceDataStoreFactory.create(produceFile = { settingsFile }))
    }

    override fun after() {
        database.close()
        settingsFile.delete()
    }
}
