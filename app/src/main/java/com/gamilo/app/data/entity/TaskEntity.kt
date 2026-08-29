package com.gamilo.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gamilo.app.data.model.TaskPriority

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = JobEntity::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("jobId"), Index("deletedAt"), Index("isDone"), Index("dueAt")],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobId: Long?,
    val title: String,
    val notes: String,
    val priority: TaskPriority,
    val isDone: Boolean,
    val doneAt: Long?,
    val dueAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
