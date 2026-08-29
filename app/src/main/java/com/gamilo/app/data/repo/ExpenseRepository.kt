package com.gamilo.app.data.repo

import androidx.paging.PagingSource
import com.gamilo.app.core.Clock
import com.gamilo.app.core.Money
import com.gamilo.app.data.dao.ExpenseDao
import com.gamilo.app.data.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao, private val clock: Clock) {
    fun pagingSource(
        jobId: Long? = null,
        unassignedOnly: Boolean = false,
        startMillis: Long = 0L,
        endMillis: Long = Long.MAX_VALUE,
    ): PagingSource<Int, ExpenseEntity> = expenseDao.pagingSource(jobId, unassignedOnly, startMillis, endMillis)
    fun observeAllIncludingDeleted(): Flow<List<ExpenseEntity>> = expenseDao.observeAllIncludingDeleted()
    suspend fun getById(id: Long): ExpenseEntity? = expenseDao.getById(id)

    suspend fun create(expense: ExpenseEntity): Long {
        val now = clock.nowMillis()
        return expenseDao.insert(
            expense.copy(
                costCad = Money.convertToCad(expense.cost, expense.fxRateApplied),
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
            ),
        )
    }

    suspend fun update(expense: ExpenseEntity) {
        expenseDao.update(
            expense.copy(
                costCad = Money.convertToCad(expense.cost, expense.fxRateApplied),
                updatedAt = clock.nowMillis(),
            ),
        )
    }

    suspend fun softDelete(id: Long) = expenseDao.softDelete(id, clock.nowMillis())
}
