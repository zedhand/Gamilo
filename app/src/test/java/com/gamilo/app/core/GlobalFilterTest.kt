package com.gamilo.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class GlobalFilterTest {

    private val noonJan15 = Instant.parse("2026-01-15T12:00:00Z")

    @Test
    fun jobFilterOption_matchesExpectedJobIds() {
        assertTrue(JobFilterOption.All.matches(null))
        assertTrue(JobFilterOption.All.matches(7L))
        assertTrue(JobFilterOption.Unassigned.matches(null))
        assertFalse(JobFilterOption.Unassigned.matches(7L))
        assertTrue(JobFilterOption.Specific(7L).matches(7L))
        assertFalse(JobFilterOption.Specific(7L).matches(8L))
        assertFalse(JobFilterOption.Specific(7L).matches(null))
    }

    @Test
    fun jobFilterOption_toQueryParams_distinguishesAllFromUnassigned() {
        assertEquals(null to false, JobFilterOption.All.toQueryParams())
        assertEquals(null to true, JobFilterOption.Unassigned.toQueryParams())
        assertEquals(7L to false, JobFilterOption.Specific(7L).toQueryParams())
    }

    @Test
    fun dateRange_today_containsOnlyThatCalendarDay() {
        val range = DateRange.forPreset(DateRangePreset.TODAY, zone = ZoneOffset.UTC, now = noonJan15)
        assertTrue(Instant.parse("2026-01-15T00:00:00Z").toEpochMilli() in range)
        assertFalse(Instant.parse("2026-01-16T00:00:00Z").toEpochMilli() in range)
        assertFalse(Instant.parse("2026-01-14T23:59:59Z").toEpochMilli() in range)
    }

    @Test
    fun dateRange_allTime_containsAnyTimestamp() {
        val range = DateRange.forPreset(DateRangePreset.ALL_TIME, now = noonJan15)
        assertTrue(0L in range)
        assertTrue(noonJan15.toEpochMilli() in range)
    }
}
