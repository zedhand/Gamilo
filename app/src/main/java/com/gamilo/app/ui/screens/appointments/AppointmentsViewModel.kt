package com.gamilo.app.ui.screens.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamilo.app.data.entity.AppointmentEntity
import com.gamilo.app.data.entity.JobEntity
import com.gamilo.app.data.repo.AppointmentRepository
import com.gamilo.app.data.repo.JobRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AppointmentsViewModel(
    private val appointmentRepository: AppointmentRepository,
    private val jobRepository: JobRepository,
) : ViewModel() {

    val appointments: Flow<List<AppointmentEntity>> = appointmentRepository.observeAll()
    val jobs: Flow<List<JobEntity>> = jobRepository.observeActive()

    fun addAppointment(title: String, startAt: Long, endAt: Long, jobId: Long?, location: String, notes: String) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return
        viewModelScope.launch {
            appointmentRepository.create(
                AppointmentEntity(
                    jobId = jobId,
                    title = trimmedTitle,
                    startAt = startAt,
                    endAt = endAt,
                    location = location.trim(),
                    notes = notes.trim(),
                    createdAt = 0,
                    updatedAt = 0,
                    deletedAt = null,
                ),
            )
        }
    }

    fun deleteAppointment(id: Long) = viewModelScope.launch { appointmentRepository.softDelete(id) }
}
