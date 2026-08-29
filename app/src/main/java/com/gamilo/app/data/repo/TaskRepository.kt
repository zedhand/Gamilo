package com.gamilo.app.data.repo

import com.gamilo.app.core.Clock
import com.gamilo.app.data.dao.TaskDao
import com.gamilo.app.data.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao, private val clock: Clock) {
    fun observeAll(): Flow<List<TaskEntity>> = taskDao.observeAll()
    fun observeForJob(jobId: Long): Flow<List<TaskEntity>> = taskDao.observeForJob(jobId)
    fun observeAllIncludingDeleted(): Flow<List<TaskEntity>> = taskDao.observeAllIncludingDeleted()
    suspend fun getById(id: Long): TaskEntity? = taskDao.getById(id)

    suspend fun create(task: TaskEntity): Long {
        val now = clock.nowMillis()
        return taskDao.insert(task.copy(createdAt = now, updatedAt = now, deletedAt = null))
    }

    suspend fun update(task: TaskEntity) = taskDao.update(task.copy(updatedAt = clock.nowMillis()))

    suspend fun setDone(task: TaskEntity, done: Boolean) {
        val now = clock.nowMillis()
        taskDao.update(task.copy(isDone = done, doneAt = if (done) now else null, updatedAt = now))
    }

    suspend fun softDelete(id: Long) = taskDao.softDelete(id, clock.nowMillis())
}
