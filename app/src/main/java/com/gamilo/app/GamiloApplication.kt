package com.gamilo.app

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.gamilo.app.backup.DailyBackupWorker
import com.gamilo.app.di.AppContainer
import com.gamilo.app.timer.NotificationChannels
import java.util.concurrent.TimeUnit

class GamiloApplication : Application() {

    /**
     * Only set once the biometric-gated unlock flow (see security/DatabaseUnlocker.kt) has
     * produced a passphrase and MainActivity has built a real [AppContainer] with it — there
     * is no eager construction here, unlike Stages 2-4, because opening the encrypted database
     * now requires a live authentication step that can't run in Application.onCreate().
     */
    lateinit var container: AppContainer
        private set

    fun attachContainer(container: AppContainer) {
        this.container = container
    }

    override fun onCreate() {
        super.onCreate()
        // Loads the native libsqlcipher.so — needed before any SupportOpenHelperFactory/
        // SQLiteDatabase use, but doesn't touch the encrypted file itself, so it's safe before
        // unlock. sqlcipher-android (unlike the old android-database-sqlcipher) loads via a
        // plain System.loadLibrary call rather than a Context-requiring static helper.
        System.loadLibrary("sqlcipher")
        NotificationChannels.ensureCreated(this)
        scheduleDailyBackup()
    }

    private fun scheduleDailyBackup() {
        // No network permission is requested at all; NOT_REQUIRED just avoids WorkManager
        // waiting on a connectivity signal the app will never produce. The worker copies the
        // encrypted database file directly — it doesn't need AppContainer or the passphrase.
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
        val request = PeriodicWorkRequestBuilder<DailyBackupWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            DailyBackupWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
