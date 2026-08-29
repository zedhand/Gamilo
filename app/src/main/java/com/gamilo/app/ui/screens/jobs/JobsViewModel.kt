package com.gamilo.app.ui.screens.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamilo.app.data.entity.JobEntity
import com.gamilo.app.data.model.JobStatus
import com.gamilo.app.data.repo.JobRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class JobsViewModel(private val jobRepository: JobRepository) : ViewModel() {

    val jobs: Flow<List<JobEntity>> = jobRepository.observeAll()

    fun addJob(clientName: String, title: String, notes: String) {
        val trimmedClient = clientName.trim()
        val trimmedTitle = title.trim()
        if (trimmedClient.isEmpty() || trimmedTitle.isEmpty()) return
        viewModelScope.launch {
            jobRepository.create(
                JobEntity(
                    clientName = trimmedClient,
                    title = trimmedTitle,
                    status = JobStatus.ACTIVE,
                    notes = notes.trim(),
                    createdAt = 0,
                    updatedAt = 0,
                    deletedAt = null,
                ),
            )
        }
    }

    fun setStatus(job: JobEntity, status: JobStatus) = viewModelScope.launch {
        jobRepository.update(job.copy(status = status))
    }

    fun deleteJob(id: Long) = viewModelScope.launch { jobRepository.softDelete(id) }
}
