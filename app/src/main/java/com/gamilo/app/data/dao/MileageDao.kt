package com.gamilo.app.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gamilo.app.data.entity.MileageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MileageDao {
    @Insert
    suspend fun insert(trip: MileageEntity): Long

    @Update
    suspend fun update(trip: MileageEntity)

    @Query("SELECT * FROM mileage WHERE id = :id")
    suspend fun getById(id: Long): MileageEntity?

    @Query(
        """
        SELECT * FROM mileage WHERE deletedAt IS NULL
        AND (:jobId IS NULL OR jobId = :jobId)
        AND (:unassignedOnly = 0 OR jobId IS NULL)
        AND occurredAt >= :startMillis AND occurredAt < :endMillis
        ORDER BY occurredAt DESC
        """,
    )
    fun pagingSource(
        jobId: Long? = null,
        unassignedOnly: Boolean = false,
        startMillis: Long = 0L,
        endMillis: Long = Long.MAX_VALUE,
    ): PagingSource<Int, MileageEntity>

    @Query("SELECT * FROM mileage ORDER BY occurredAt DESC")
    fun observeAllIncludingDeleted(): Flow<List<MileageEntity>>

    @Query("UPDATE mileage SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long)
}
