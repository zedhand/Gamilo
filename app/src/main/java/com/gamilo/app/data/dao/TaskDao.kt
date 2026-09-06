package com.gamilo.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gamilo.app.data.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL ORDER BY isDone ASC, dueAt ASC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL AND jobId = :jobId ORDER BY isDone ASC, dueAt ASC")
    fun observeForJob(jobId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY dueAt ASC")
    fun observeAllIncludingDeleted(): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long)
}
