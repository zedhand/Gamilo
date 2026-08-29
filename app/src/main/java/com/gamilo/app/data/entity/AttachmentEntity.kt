package com.gamilo.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gamilo.app.data.model.AttachmentOwnerType

/** A local photo (content:// URI) tied to a Job, Material, or Shipping record. */
@Entity(
    tableName = "attachments",
    indices = [Index(value = ["ownerType", "ownerId"]), Index("deletedAt")],
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerType: AttachmentOwnerType,
    val ownerId: Long,
    val uri: String,
    val label: String,
    val capturedAt: Long,
    val createdAt: Long,
    val deletedAt: Long?,
)
