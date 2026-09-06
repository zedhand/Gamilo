package com.gamilo.app.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gamilo.app.data.entity.HourEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HourDao {
    @Insert
    suspend fun insert(hour: HourEntity): Long

    @Update
    suspend fun update(hour: HourEntity)

    @Query("SELECT * FROM hours WHERE id = :id")
    suspend fun getById(id: Long): HourEntity?

    /** The currently running shift (Home's Start/End Shift button), if any. */
    @Query("SELECT * FROM hours WHERE deletedAt IS NULL AND endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getOpenSession(): HourEntity?

    @Query("SELECT * FROM hours WHERE deletedAt IS NULL AND endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeOpenSession(): Flow<HourEntity?>

    @Query(
        """
        SELECT * FROM hours WHERE deletedAt IS NULL
        AND (:jobId IS NULL OR jobId = :jobId)
        AND (:unassignedOnly = 0 OR jobId IS NULL)
        AND startedAt >= :startMillis AND startedAt < :endMillis
        ORDER BY startedAt DESC
        """,
    )
    fun pagingSource(
        jobId: Long? = null,
        unassignedOnly: Boolean = false,
        startMillis: Long = 0L,
        endMillis: Long = Long.MAX_VALUE,
    ): PagingSource<Int, HourEntity>

    @Query("SELECT * FROM hours ORDER BY startedAt DESC")
    fun observeAllIncludingDeleted(): Flow<List<HourEntity>>

    @Query("UPDATE hours SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long)
}
