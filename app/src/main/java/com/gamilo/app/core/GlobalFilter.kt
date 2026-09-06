package com.gamilo.app.core

import java.time.Instant

/** Selects which jobs a filtered list should include. */
sealed class JobFilterOption {
    object All : JobFilterOption()
    object Unassigned : JobFilterOption()
    data class Specific(val jobId: Long) : JobFilterOption()

    fun matches(jobId: Long?): Boolean = when (this) {
        All -> true
        Unassigned -> jobId == null
        is Specific -> jobId == this.jobId
    }

    /**
     * The (jobId, unassignedOnly) pair a Paging DAO query needs: a plain nullable jobId can't
     * distinguish "no filter" from "match only unassigned rows", since both would otherwise
     * pass jobId = null. The query itself applies `(:jobId IS NULL OR jobId = :jobId) AND
     * (:unassignedOnly = 0 OR jobId IS NULL)` — see HourDao/ExpenseDao/MileageDao.
     */
    fun toQueryParams(): Pair<Long?, Boolean> = when (this) {
        All -> null to false
        Unassigned -> null to true
        is Specific -> jobId to false
    }
}

/**
 * App-wide sticky filter state: date range and job. Hoisted once at the root (MainActivity) and
 * threaded into every data-heavy tab (Tasks, Hours, Expenses, Mileage, Shipping) so they all
 * filter the same way, per the master plan's "Global Filtering" requirement. Home, Jobs, and
 * Settings deliberately don't take a filter — they aren't lists of transactional entries.
 */
data class GlobalFilter(
    val dateRangePreset: DateRangePreset = DateRangePreset.ALL_TIME,
    val jobFilter: JobFilterOption = JobFilterOption.All,
) {
    fun dateRange(now: Instant = Instant.now()): DateRange = DateRange.forPreset(dateRangePreset, now = now)
}
