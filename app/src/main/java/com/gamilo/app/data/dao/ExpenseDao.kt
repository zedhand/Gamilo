package com.gamilo.app.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gamilo.app.data.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): ExpenseEntity?

    @Query(
        """
        SELECT * FROM expenses WHERE deletedAt IS NULL
        AND (:jobId IS NULL OR jobId = :jobId)
        AND (:unassignedOnly = 0 OR jobId IS NULL)
        AND purchasedAt >= :startMillis AND purchasedAt < :endMillis
        ORDER BY purchasedAt DESC
        """,
    )
    fun pagingSource(
        jobId: Long? = null,
        unassignedOnly: Boolean = false,
        startMillis: Long = 0L,
        endMillis: Long = Long.MAX_VALUE,
    ): PagingSource<Int, ExpenseEntity>

    @Query("SELECT * FROM expenses ORDER BY purchasedAt DESC")
    fun observeAllIncludingDeleted(): Flow<List<ExpenseEntity>>

    @Query("UPDATE expenses SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long)
}
