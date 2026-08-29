package com.gamilo.app.data.repo

import com.gamilo.app.core.Clock
import com.gamilo.app.data.dao.JobDao
import com.gamilo.app.data.entity.JobEntity
import kotlinx.coroutines.flow.Flow

class JobRepository(private val jobDao: JobDao, private val clock: Clock) {
    fun observeAll(): Flow<List<JobEntity>> = jobDao.observeAll()
    fun observeActive(): Flow<List<JobEntity>> = jobDao.observeActive()
    fun observeAllIncludingDeleted(): Flow<List<JobEntity>> = jobDao.observeAllIncludingDeleted()
    suspend fun getById(id: Long): JobEntity? = jobDao.getById(id)

    suspend fun create(job: JobEntity): Long {
        val now = clock.nowMillis()
        return jobDao.insert(job.copy(createdAt = now, updatedAt = now, deletedAt = null))
    }

    suspend fun update(job: JobEntity) = jobDao.update(job.copy(updatedAt = clock.nowMillis()))

    suspend fun softDelete(id: Long) = jobDao.softDelete(id, clock.nowMillis())
}
