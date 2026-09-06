package com.gamilo.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gamilo.app.data.entity.AttachmentEntity
import com.gamilo.app.data.model.AttachmentOwnerType
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Insert
    suspend fun insert(attachment: AttachmentEntity): Long

    @Query("SELECT * FROM attachments WHERE deletedAt IS NULL AND ownerType = :ownerType AND ownerId = :ownerId ORDER BY capturedAt DESC")
    fun observeFor(ownerType: AttachmentOwnerType, ownerId: Long): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments ORDER BY capturedAt DESC")
    fun observeAllIncludingDeleted(): Flow<List<AttachmentEntity>>

    @Query("UPDATE attachments SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long)
}
