package com.gamilo.app.ai

import android.app.ActivityManager
import android.content.Context

/**
 * Phase 2 (AI Convenience Layer) requirement: voice/AI features must stay hidden on hardware
 * that can't run them acceptably. Gates on 6GB+ total RAM — running a Whisper TFLite inference
 * pass on a low-memory device would be unacceptably slow or risk getting the process killed.
 * This is a pure capability check — it never affects whether the manual (Phase 1) UI is
 * available, only whether the voice accelerator appears.
 */
object HardwareGate {
    private const val MIN_RAM_BYTES = 6L * 1024 * 1024 * 1024 // 6 GB

    data class Result(
        val isEligible: Boolean,
        val meetsRam: Boolean,
        val totalRamBytes: Long,
    )

    fun evaluate(context: Context): Result {
        val totalRam = totalRamBytes(context)
        val meetsRam = totalRam >= MIN_RAM_BYTES

        return Result(
            isEligible = meetsRam,
            meetsRam = meetsRam,
            totalRamBytes = totalRam,
        )
    }

    private fun totalRamBytes(context: Context): Long {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 0L
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        return info.totalMem
    }
}
