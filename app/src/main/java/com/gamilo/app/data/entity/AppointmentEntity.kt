package com.gamilo.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A scheduled appointment/visit, optionally tied to a job — the local-first Phase 5 calendar. */
@Entity(
    tableName = "appointments",
    foreignKeys = [
        ForeignKey(
            entity = JobEntity::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("jobId"), Index("startAt"), Index("deletedAt")],
)
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobId: Long?,
    val title: String,
    val startAt: Long,
    val endAt: Long,
    val location: String,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
