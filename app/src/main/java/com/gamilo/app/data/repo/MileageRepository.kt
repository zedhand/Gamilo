package com.gamilo.app.data.repo

import androidx.paging.PagingSource
import com.gamilo.app.core.Clock
import com.gamilo.app.core.Money
import com.gamilo.app.data.dao.MileageDao
import com.gamilo.app.data.entity.MileageEntity
import kotlinx.coroutines.flow.Flow

class MileageRepository(private val mileageDao: MileageDao, private val clock: Clock) {
    fun pagingSource(
        jobId: Long? = null,
        unassignedOnly: Boolean = false,
        startMillis: Long = 0L,
        endMillis: Long = Long.MAX_VALUE,
    ): PagingSource<Int, MileageEntity> = mileageDao.pagingSource(jobId, unassignedOnly, startMillis, endMillis)
    fun observeAllIncludingDeleted(): Flow<List<MileageEntity>> = mileageDao.observeAllIncludingDeleted()
    suspend fun getById(id: Long): MileageEntity? = mileageDao.getById(id)

    private fun computeAmountCad(trip: MileageEntity) =
        Money.convertToCad(trip.distanceKm.multiply(trip.mileageRateApplied), trip.fxRateApplied)

    suspend fun create(trip: MileageEntity): Long {
        val now = clock.nowMillis()
        return mileageDao.insert(
            trip.copy(amountCad = computeAmountCad(trip), createdAt = now, updatedAt = now, deletedAt = null),
        )
    }

    suspend fun update(trip: MileageEntity) {
        mileageDao.update(trip.copy(amountCad = computeAmountCad(trip), updatedAt = clock.nowMillis()))
    }

    suspend fun softDelete(id: Long) = mileageDao.softDelete(id, clock.nowMillis())
}
