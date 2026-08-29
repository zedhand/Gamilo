package com.gamilo.app.backup

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.gamilo.app.core.FakeClock
import com.gamilo.app.data.GamiloDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
@SmallTest
class BackupManagerTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val clock = FakeClock(1_000L)
    private lateinit var backupManager: BackupManager
    private lateinit var dbFile: File

    @Before
    fun setUp() {
        backupManager = BackupManager(context, clock)
        dbFile = context.getDatabasePath(GamiloDatabase.DATABASE_NAME)
        dbFile.parentFile?.mkdirs()
        dbFile.writeText("fake-encrypted-db-content")
    }

    @After
    fun tearDown() {
        dbFile.delete()
        File(context.filesDir, "backups").deleteRecursively()
    }

    @Test
    fun performRollingBackup_copiesTheLiveDbFile() {
        val backup = backupManager.performRollingBackup()
        assertTrue(backup.exists())
        assertEquals(dbFile.readText(), backup.readText())
    }

    @Test
    fun performRollingBackup_prunesOldestBeyondTheKeepLimit() {
        repeat(5) {
            clock.advanceBy(1_000L)
            backupManager.performRollingBackup(maxBackupsToKeep = 3)
        }
        assertEquals(3, backupManager.listBackups().size)
    }

    @Test
    fun exportTo_writesTheLiveDbFileBytes() {
        val output = ByteArrayOutputStream()
        backupManager.exportTo(output)
        assertEquals("fake-encrypted-db-content", output.toString())
    }

    @Test
    fun importFrom_overwritesTheLiveDbFile() {
        val imported = "restored-content".byteInputStream()
        backupManager.importFrom(imported)
        assertEquals("restored-content", dbFile.readText())
    }
}
