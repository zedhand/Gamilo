package com.gamilo.app.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.gamilo.app.backup.BackupManager
import com.gamilo.app.core.Clock
import com.gamilo.app.core.SystemClock
import com.gamilo.app.data.GamiloDatabase
import com.gamilo.app.data.repo.AppointmentRepository
import com.gamilo.app.data.repo.AttachmentRepository
import com.gamilo.app.data.repo.ExpenseRepository
import com.gamilo.app.data.repo.HourRepository
import com.gamilo.app.data.repo.JobRepository
import com.gamilo.app.data.repo.MileageRepository
import com.gamilo.app.data.repo.ShippingRepository
import com.gamilo.app.data.repo.TaskRepository
import com.gamilo.app.export.DataExportService
import com.gamilo.app.settings.SettingsStore
import net.sqlcipher.database.SupportFactory

private val Context.dataStore by preferencesDataStore(name = "gamilo_settings")

/**
 * Hand-rolled DI container — no Hilt/Dagger. Built only after [passphrase] has been derived by
 * the biometric-gated unlock flow (see security/DatabaseUnlocker.kt) — there is no path to a
 * working container without passing through that gate first. One instance lives on
 * [com.gamilo.app.GamiloApplication] for the process lifetime; tests construct their own
 * container (or individual repositories) against a plain in-memory database instead of
 * touching SQLCipher or this class at all.
 */
class AppContainer(context: Context, passphrase: ByteArray, val clock: Clock = SystemClock) {

    private val appContext = context.applicationContext

    val database: GamiloDatabase = Room.databaseBuilder(
        appContext,
        GamiloDatabase::class.java,
        GamiloDatabase.DATABASE_NAME,
    )
        .openHelperFactory(SupportFactory(passphrase))
        // No release has ever shipped (Stage 6 is still pending) — there's no real user data
        // anywhere to preserve across a schema bump yet, so a destructive fallback is safe.
        // Revisit this the moment Stage 6 ships a real Migration path becomes mandatory.
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()

    val settingsStore: SettingsStore = SettingsStore(appContext.dataStore)
    val backupManager: BackupManager by lazy { BackupManager(appContext, clock) }

    val jobRepository: JobRepository by lazy { JobRepository(database.jobDao(), clock) }
    val taskRepository: TaskRepository by lazy { TaskRepository(database.taskDao(), clock) }
    val hourRepository: HourRepository by lazy { HourRepository(database.hourDao(), clock) }
    val expenseRepository: ExpenseRepository by lazy { ExpenseRepository(database.expenseDao(), clock) }
    val mileageRepository: MileageRepository by lazy { MileageRepository(database.mileageDao(), clock) }
    val shippingRepository: ShippingRepository by lazy { ShippingRepository(database.shippingDao(), clock) }
    val attachmentRepository: AttachmentRepository by lazy { AttachmentRepository(database.attachmentDao(), clock) }
    val appointmentRepository: AppointmentRepository by lazy { AppointmentRepository(database.appointmentDao(), clock) }

    val dataExportService: DataExportService by lazy {
        DataExportService(
            jobRepository, taskRepository, hourRepository, expenseRepository,
            mileageRepository, shippingRepository, attachmentRepository, appointmentRepository,
        )
    }
}
