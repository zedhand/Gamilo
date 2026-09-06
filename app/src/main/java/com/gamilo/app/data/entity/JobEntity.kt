package com.gamilo.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gamilo.app.data.model.JobStatus

/** A "Gig" — the container Tasks, Hours, Materials, Mileage, and Shipping records optionally tag. */
@Entity(
    tableName = "jobs",
    indices = [Index("deletedAt"), Index("status")],
)
data class JobEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientName: String,
    val title: String,
    val status: JobStatus,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
