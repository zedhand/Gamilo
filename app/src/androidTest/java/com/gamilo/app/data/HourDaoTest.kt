package com.gamilo.app.data

import androidx.paging.PagingSource
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.gamilo.app.data.entity.HourEntity
import com.gamilo.app.data.entity.JobEntity
import com.gamilo.app.data.model.JobStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
@SmallTest
class HourDaoTest {

    @get:Rule
    val dbRule = DbTestRule()

    private val dao get() = dbRule.database.hourDao()

    private fun hour(
        jobId: Long? = null,
        startedAt: Long = 1_000L,
        endedAt: Long? = 2_000L,
    ) = HourEntity(
        jobId = jobId,
        startedAt = startedAt,
        endedAt = endedAt,
        hourlyRate = BigDecimal("45.00"),
        currencyCode = "CAD",
        fxRateApplied = BigDecimal.ONE,
        hourlyRateCad = BigDecimal("45.00"),
        gstRateApplied = BigDecimal("0.05"),
        pstRateApplied = BigDecimal("0.07"),
        notes = "",
        createdAt = 1_000L,
        updatedAt = 1_000L,
        deletedAt = null,
    )

    @Test
    fun getOpenSession_returnsRowWithNullEndedAt() = runTest {
        dao.insert(hour(endedAt = 3_000L))
        dao.insert(hour(endedAt = null))

        val open = dao.getOpenSession()
        assertEquals(null, open?.endedAt)
    }

    @Test
    fun getOpenSession_returnsNullWhenNoneRunning() = runTest {
        dao.insert(hour(endedAt = 3_000L))
        assertNull(dao.getOpenSession())
    }

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
    fun pagingSource_filtersByJobIdWhenProvided() = runTest {
        val jobA = insertJob()
        val jobB = insertJob()
        dao.insert(hour(jobId = jobA))
        dao.insert(hour(jobId = jobB))

        val result = dao.pagingSource(jobA).load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 10, placeholdersEnabled = false),
        ) as PagingSource.LoadResult.Page
        assertEquals(1, result.data.size)
    }

    @Test
    fun pagingSource_returnsAllWhenJobIdNull() = runTest {
        val jobA = insertJob()
        val jobB = insertJob()
        dao.insert(hour(jobId = jobA))
        dao.insert(hour(jobId = jobB))

        val result = dao.pagingSource(null).load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 10, placeholdersEnabled = false),
        ) as PagingSource.LoadResult.Page
        assertEquals(2, result.data.size)
    }

    @Test
    fun pagingSource_excludesSoftDeletedRows() = runTest {
        val id = dao.insert(hour())
        dao.softDelete(id, deletedAt = 5_000L)

        val result = dao.pagingSource(null).load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 10, placeholdersEnabled = false),
        ) as PagingSource.LoadResult.Page
        assertTrue(result.data.isEmpty())
    }
}
