package com.gamilo.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gamilo.app.data.entity.ShippingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShippingDao {
    @Insert
    suspend fun insert(shipment: ShippingEntity): Long

    @Update
    suspend fun update(shipment: ShippingEntity)

    @Query("SELECT * FROM shipping WHERE id = :id")
    suspend fun getById(id: Long): ShippingEntity?

    @Query("SELECT * FROM shipping WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ShippingEntity>>

    @Query("SELECT * FROM shipping WHERE deletedAt IS NULL AND jobId = :jobId ORDER BY createdAt DESC")
    fun observeForJob(jobId: Long): Flow<List<ShippingEntity>>

    @Query("SELECT * FROM shipping ORDER BY createdAt DESC")
    fun observeAllIncludingDeleted(): Flow<List<ShippingEntity>>

    @Query("UPDATE shipping SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long)
}
