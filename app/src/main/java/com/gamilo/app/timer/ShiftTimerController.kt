package com.gamilo.app.timer

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class ShiftTimerController(private val context: Context) {
    fun start(startedAtMillis: Long) {
        ContextCompat.startForegroundService(context, ShiftTimerService.startIntent(context, startedAtMillis))
    }

    fun stop() {
        context.stopService(Intent(context, ShiftTimerService::class.java))
    }
}
