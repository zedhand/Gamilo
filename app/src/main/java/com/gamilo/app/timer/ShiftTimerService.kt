package com.gamilo.app.timer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat

/** Persistent notification for Home's Start/End Shift button — required so a shift stays visibly tracked in the status bar. */
class ShiftTimerService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val startedAt = intent?.getLongExtra(EXTRA_STARTED_AT, System.currentTimeMillis()) ?: System.currentTimeMillis()
        val notification = NotificationCompat.Builder(this, NotificationChannels.SHIFT_TIMER_CHANNEL_ID)
            .setContentTitle("Shift active")
            .setContentText("Started at ${DateFormat.format("HH:mm", startedAt)}")
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_STICKY
    }

    companion object {
        private const val EXTRA_STARTED_AT = "started_at"
        private const val NOTIFICATION_ID = 1001

        fun startIntent(context: Context, startedAtMillis: Long): Intent =
            Intent(context, ShiftTimerService::class.java).putExtra(EXTRA_STARTED_AT, startedAtMillis)
    }
}
