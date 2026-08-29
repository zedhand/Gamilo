package com.gamilo.app.data

import androidx.paging.PagingSource
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.gamilo.app.data.entity.ExpenseEntity
import com.gamilo.app.data.entity.JobEntity
import com.gamilo.app.data.model.JobStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
@SmallTest
class ExpenseDaoTest {

    @get:Rule
    val dbRule = DbTestRule()

    private val dao get() = dbRule.database.expenseDao()

    private fun expense(jobId: Long? = null, description: String = "2x4 lumber", purchasedAt: Long = 1_000L) =
        ExpenseEntity(
            jobId = jobId,
            description = description,
            cost = BigDecimal("19.99"),
            currencyCode = "CAD",
            fxRateApplied = BigDecimal.ONE,
            costCad = BigDecimal("19.99"),
            gstRateApplied = BigDecimal("0.05"),
            pstRateApplied = BigDecimal("0.07"),
            photoUri = null,
            purchasedAt = purchasedAt,
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
    fun pagingSource_filtersByJobIdWhenProvided() = runTest {
        val jobA = insertJob()
        val jobB = insertJob()
        dao.insert(expense(jobId = jobA))
        dao.insert(expense(jobId = jobB))

        val result = dao.pagingSource(jobA).load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 10, placeholdersEnabled = false),
        ) as PagingSource.LoadResult.Page
        assertEquals(1, result.data.size)
    }

    @Test
    fun pagingSource_ordersByPurchasedAtDescending() = runTest {
        dao.insert(expense(purchasedAt = 1_000L))
        dao.insert(expense(purchasedAt = 3_000L))
        dao.insert(expense(purchasedAt = 2_000L))

        val result = dao.pagingSource(null).load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 10, placeholdersEnabled = false),
        ) as PagingSource.LoadResult.Page
        assertEquals(listOf(3_000L, 2_000L, 1_000L), result.data.map { it.purchasedAt })
    }

    @Test
    fun photoUriIsNullable_whenNoReceiptCaptured() = runTest {
        val id = dao.insert(expense())
        assertEquals(null, dao.getById(id)?.photoUri)
    }

    @Test
    fun softDeletedExpense_isExcludedFromPagingSource() = runTest {
        val id = dao.insert(expense())
        dao.softDelete(id, deletedAt = 5_000L)

        val result = dao.pagingSource(null).load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 10, placeholdersEnabled = false),
        ) as PagingSource.LoadResult.Page
        assertTrue(result.data.isEmpty())
    }
}
