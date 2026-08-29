package com.gamilo.app.core

import java.time.Instant
import java.time.ZoneId

enum class DateRangePreset(val label: String) {
    TODAY("Today"),
    THIS_WEEK("Week"),
    THIS_MONTH("Month"),
    ALL_TIME("All"),
}

/** Half-open [startMillis, endMillis) range in the device's local zone. */
data class DateRange(val startMillis: Long, val endMillis: Long) {
    operator fun contains(epochMillis: Long): Boolean = epochMillis in startMillis until endMillis

    companion object {
        fun forPreset(preset: DateRangePreset, zone: ZoneId = ZoneId.systemDefault(), now: Instant = Instant.now()): DateRange {
            val today = now.atZone(zone).toLocalDate()
            return when (preset) {
                DateRangePreset.TODAY -> ofLocalDates(today, today.plusDays(1), zone)
                DateRangePreset.THIS_WEEK -> {
                    val startOfWeek = today.minusDays((today.dayOfWeek.value - 1).toLong())
                    ofLocalDates(startOfWeek, startOfWeek.plusWeeks(1), zone)
                }
                DateRangePreset.THIS_MONTH -> {
                    val start = today.withDayOfMonth(1)
                    ofLocalDates(start, start.plusMonths(1), zone)
                }
                DateRangePreset.ALL_TIME -> DateRange(0L, Long.MAX_VALUE)
            }
        }

        private fun ofLocalDates(start: java.time.LocalDate, endExclusive: java.time.LocalDate, zone: ZoneId): DateRange =
            DateRange(
                start.atStartOfDay(zone).toInstant().toEpochMilli(),
                endExclusive.atStartOfDay(zone).toInstant().toEpochMilli(),
            )
    }
}
