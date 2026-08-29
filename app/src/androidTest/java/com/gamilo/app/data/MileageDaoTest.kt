package com.gamilo.app.data

import androidx.paging.PagingSource
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.gamilo.app.data.entity.JobEntity
import com.gamilo.app.data.entity.MileageEntity
import com.gamilo.app.data.model.JobStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
@SmallTest
class MileageDaoTest {

    @get:Rule
    val dbRule = DbTestRule()

    private val dao get() = dbRule.database.mileageDao()

    private fun trip(jobId: Long? = null, occurredAt: Long = 1_000L) = MileageEntity(
        jobId = jobId,
        occurredAt = occurredAt,
        originLabel = "Shop",
        destinationLabel = "Client site",
        distanceKm = BigDecimal("12.5"),
        mileageRateApplied = BigDecimal("0.68"),
        currencyCode = "CAD",
        fxRateApplied = BigDecimal.ONE,
        amountCad = BigDecimal("8.50"),
        notes = "",
        createdAt = 1_000L,
        updatedAt = 1_000L,
        deletedAt = null,
    )

    private suspend fun insertJob(): Long = dbRule.database.jobDao().insert(
        JobEntity(
            clientName = "Acme Co",
            title = "Deck rebuild",
            status = JobStatus.ACTIVE,
            notes = "",
            createdAt = 1_000L,
            updatedAt = 1_000L,
            deletedAt = null,
        ),
    )

    @Test
    fun pagingSource_filtersByJobId() = runTest {
        val jobA = insertJob()
        val jobB = insertJob()
        dao.insert(trip(jobId = jobA))
        dao.insert(trip(jobId = jobB))

        val result = dao.pagingSource(jobB).load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 10, placeholdersEnabled = false),
        ) as PagingSource.LoadResult.Page
        assertEquals(1, result.data.size)
    }

    @Test
    fun insertAndGetById_roundTripsDistanceAndRate() = runTest {
        val id = dao.insert(trip())
        val loaded = dao.getById(id)
        assertEquals(BigDecimal("12.5"), loaded?.distanceKm)
        assertEquals(BigDecimal("0.68"), loaded?.mileageRateApplied)
    }
}
