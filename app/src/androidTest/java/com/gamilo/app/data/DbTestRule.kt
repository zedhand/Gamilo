package com.gamilo.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.rules.ExternalResource

/** In-memory Room DB per test, so every instrumented DAO test starts from an empty schema. */
class DbTestRule : ExternalResource() {
    lateinit var database: GamiloDatabase
        private set

    override fun before() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GamiloDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    override fun after() {
        database.close()
    }
}
