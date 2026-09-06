package com.gamilo.app.core

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** ISO-8601-ish local timestamps for CSV export — self-describing to a reader without the app. */
object TimeFormat {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun formatDateTime(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        formatter.format(Instant.ofEpochMilli(epochMillis).atZone(zone))
}
