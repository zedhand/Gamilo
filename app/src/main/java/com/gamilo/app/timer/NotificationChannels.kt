package com.gamilo.app.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationChannels {
    const val SHIFT_TIMER_CHANNEL_ID = "shift_timer"

    fun ensureCreated(context: Context) {
        val channel = NotificationChannel(
            SHIFT_TIMER_CHANNEL_ID,
            "Shift Timer",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Persistent notification while a shift is active"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
