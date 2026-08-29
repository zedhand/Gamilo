package com.gamilo.app.data.repo

import com.gamilo.app.core.Clock
import com.gamilo.app.data.dao.AppointmentDao
import com.gamilo.app.data.entity.AppointmentEntity
import kotlinx.coroutines.flow.Flow

class AppointmentRepository(private val appointmentDao: AppointmentDao, private val clock: Clock) {
    fun observeAll(): Flow<List<AppointmentEntity>> = appointmentDao.observeAll()
    fun observeInRange(startMillis: Long, endMillis: Long): Flow<List<AppointmentEntity>> =
        appointmentDao.observeInRange(startMillis, endMillis)
    fun observeAllIncludingDeleted(): Flow<List<AppointmentEntity>> = appointmentDao.observeAllIncludingDeleted()
    suspend fun getById(id: Long): AppointmentEntity? = appointmentDao.getById(id)

    suspend fun create(appointment: AppointmentEntity): Long {
        val now = clock.nowMillis()
        return appointmentDao.insert(appointment.copy(createdAt = now, updatedAt = now, deletedAt = null))
    }

    suspend fun update(appointment: AppointmentEntity) = appointmentDao.update(appointment.copy(updatedAt = clock.nowMillis()))

    suspend fun softDelete(id: Long) = appointmentDao.softDelete(id, clock.nowMillis())
}
