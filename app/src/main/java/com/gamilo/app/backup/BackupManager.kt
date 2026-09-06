package com.gamilo.app.backup

import android.content.Context
import com.gamilo.app.core.Clock
import com.gamilo.app.data.GamiloDatabase
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * SQLCipher pages are ciphertext on disk, so every operation here is a plain file copy — no
 * extra encryption step is needed for a backup or SAF export to be "unreadable if pulled
 * directly off the device". The `-wal`/`-shm` sidecar files (if present) are copied alongside
 * the main file so a database that hasn't been checkpointed is still fully recoverable — see
 * SQLite's WAL-mode documentation. Rolling background backups don't checkpoint first (the
 * worker that calls [performRollingBackup] may run in a process where the database isn't even
 * open); a manual export from Settings does checkpoint first, since the live database
 * connection is guaranteed available there — see SettingsViewModel.exportBackup.
 */
class BackupManager(private val context: Context, private val clock: Clock) {

    private val backupsDir: File get() = File(context.filesDir, "backups").apply { mkdirs() }

    private val liveDbFile: File get() = context.getDatabasePath(GamiloDatabase.DATABASE_NAME)

    fun performRollingBackup(maxBackupsToKeep: Int = 7): File {
        val destination = File(backupsDir, "gamilo_backup_${clock.nowMillis()}.db")
        copyWithSidecars(liveDbFile, destination)
        pruneOldBackups(maxBackupsToKeep)
        return destination
    }

    fun listBackups(): List<File> =
        backupsDir.listFiles { file -> file.name.startsWith("gamilo_backup_") && file.name.endsWith(".db") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    /** Caller is responsible for checkpointing the live database first if a consistent snapshot matters. */
    fun exportTo(outputStream: OutputStream) {
        liveDbFile.inputStream().use { it.copyTo(outputStream) }
    }

    /** Overwrites the live database file. Caller must close the Room database first and reopen (or restart) after. */
    fun importFrom(inputStream: InputStream) {
        File(liveDbFile.path + "-wal").delete()
        File(liveDbFile.path + "-shm").delete()
        liveDbFile.outputStream().use { output -> inputStream.copyTo(output) }
    }

    /**
     * Deletes the live database file and every rolling backup. Caller must close the Room
     * database first (same contract as [importFrom]) and restart the app afterward — a fresh
     * cold start recreates an empty encrypted database against the same already-stored
     * passphrase, so nothing about the Keystore/DataStore passphrase needs to change here.
     */
    fun wipeAllData() {
        liveDbFile.delete()
        File(liveDbFile.path + "-wal").delete()
        File(liveDbFile.path + "-shm").delete()
        backupsDir.listFiles()?.forEach { it.delete() }
    }

    private fun copyWithSidecars(source: File, destination: File) {
        if (!source.exists()) return
        source.copyTo(destination, overwrite = true)
        File(source.path + "-wal").takeIf { it.exists() }
            ?.copyTo(File(destination.path + "-wal"), overwrite = true)
        File(source.path + "-shm").takeIf { it.exists() }
            ?.copyTo(File(destination.path + "-shm"), overwrite = true)
    }

    private fun pruneOldBackups(maxToKeep: Int) {
        listBackups().drop(maxToKeep).forEach { backup ->
            backup.delete()
            File(backup.path + "-wal").delete()
            File(backup.path + "-shm").delete()
        }
    }
}
