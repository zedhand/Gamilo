package com.gamilo.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gamilo.app.data.entity.JobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Insert
    suspend fun insert(job: JobEntity): Long

    @Update
    suspend fun update(job: JobEntity)

    @Query("SELECT * FROM jobs WHERE id = :id")
    suspend fun getById(id: Long): JobEntity?

    @Query("SELECT * FROM jobs WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs WHERE deletedAt IS NULL AND status = 'ACTIVE' ORDER BY updatedAt DESC")
    fun observeActive(): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs ORDER BY updatedAt DESC")
    fun observeAllIncludingDeleted(): Flow<List<JobEntity>>

    @Query("UPDATE jobs SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long)
}
