package com.gamilo.app.data.repo

import androidx.paging.PagingSource
import com.gamilo.app.core.Clock
import com.gamilo.app.core.Money
import com.gamilo.app.data.dao.HourDao
import com.gamilo.app.data.entity.HourEntity
import kotlinx.coroutines.flow.Flow

class HourRepository(private val hourDao: HourDao, private val clock: Clock) {
    fun pagingSource(
        jobId: Long? = null,
        unassignedOnly: Boolean = false,
        startMillis: Long = 0L,
        endMillis: Long = Long.MAX_VALUE,
    ): PagingSource<Int, HourEntity> = hourDao.pagingSource(jobId, unassignedOnly, startMillis, endMillis)
    fun observeAllIncludingDeleted(): Flow<List<HourEntity>> = hourDao.observeAllIncludingDeleted()
    suspend fun getById(id: Long): HourEntity? = hourDao.getById(id)
    suspend fun getOpenSession(): HourEntity? = hourDao.getOpenSession()
    fun observeOpenSession(): Flow<HourEntity?> = hourDao.observeOpenSession()

    suspend fun create(hour: HourEntity): Long {
        val now = clock.nowMillis()
        return hourDao.insert(
            hour.copy(
                hourlyRateCad = Money.convertToCad(hour.hourlyRate, hour.fxRateApplied),
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
            ),
        )
    }

    suspend fun update(hour: HourEntity) {
        hourDao.update(
            hour.copy(
                hourlyRateCad = Money.convertToCad(hour.hourlyRate, hour.fxRateApplied),
                updatedAt = clock.nowMillis(),
            ),
        )
    }

    /** Starts a shift: fails fast if one is already running — callers should check [getOpenSession] first. */
    suspend fun startShift(jobId: Long?, hourlyRate: java.math.BigDecimal, currencyCode: String, fxRateApplied: java.math.BigDecimal, gstRateApplied: java.math.BigDecimal, pstRateApplied: java.math.BigDecimal): Long {
        check(getOpenSession() == null) { "A shift is already running" }
        val now = clock.nowMillis()
        return create(
            HourEntity(
                jobId = jobId,
                startedAt = now,
                endedAt = null,
                hourlyRate = hourlyRate,
                currencyCode = currencyCode,
                fxRateApplied = fxRateApplied,
                hourlyRateCad = java.math.BigDecimal.ZERO,
                gstRateApplied = gstRateApplied,
                pstRateApplied = pstRateApplied,
                notes = "",
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
            ),
        )
    }

    suspend fun endShift(session: HourEntity) {
        require(session.endedAt == null) { "Session ${session.id} is already ended" }
        update(session.copy(endedAt = clock.nowMillis()))
    }

    suspend fun softDelete(id: Long) = hourDao.softDelete(id, clock.nowMillis())
}
