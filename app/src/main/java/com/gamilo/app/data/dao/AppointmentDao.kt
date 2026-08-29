package com.gamilo.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gamilo.app.data.entity.AppointmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {
    @Insert
    suspend fun insert(appointment: AppointmentEntity): Long

    @Update
    suspend fun update(appointment: AppointmentEntity)

    @Query("SELECT * FROM appointments WHERE id = :id")
    suspend fun getById(id: Long): AppointmentEntity?

    @Query("SELECT * FROM appointments WHERE deletedAt IS NULL ORDER BY startAt ASC")
    fun observeAll(): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE deletedAt IS NULL AND startAt >= :startMillis AND startAt < :endMillis ORDER BY startAt ASC")
    fun observeInRange(startMillis: Long, endMillis: Long): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments ORDER BY startAt ASC")
    fun observeAllIncludingDeleted(): Flow<List<AppointmentEntity>>

    @Query("UPDATE appointments SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long)
}
