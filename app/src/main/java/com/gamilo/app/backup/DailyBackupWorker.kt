package com.gamilo.app.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gamilo.app.core.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DailyBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runCatching { BackupManager(applicationContext, SystemClock).performRollingBackup() }
            .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
    }

    companion object {
        const val UNIQUE_WORK_NAME = "gamilo_daily_backup"
    }
}
