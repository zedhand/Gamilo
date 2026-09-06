package com.gamilo.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamilo.app.data.entity.HourEntity
import com.gamilo.app.data.entity.JobEntity
import com.gamilo.app.data.repo.HourRepository
import com.gamilo.app.data.repo.JobRepository
import com.gamilo.app.data.repo.TaskRepository
import com.gamilo.app.settings.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class HomeViewModel(
    private val jobRepository: JobRepository,
    private val taskRepository: TaskRepository,
    private val hourRepository: HourRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    val activeJobs: Flow<List<JobEntity>> = jobRepository.observeActive()
    val openTasksCount: Flow<Int> = taskRepository.observeAll().map { tasks -> tasks.count { !it.isDone } }
    val openSession: Flow<HourEntity?> = hourRepository.observeOpenSession()

    /** Starts a shift seeded from Settings' default rates, or ends the currently running one. */
    fun toggleShift() = viewModelScope.launch {
        val current = hourRepository.getOpenSession()
        if (current == null) {
            val settings = settingsStore.settings.first()
            hourRepository.startShift(
                jobId = null,
                hourlyRate = settings.defaultHourlyRate,
                currencyCode = settings.baseCurrencyCode,
                fxRateApplied = settings.currentFxRateApplied,
                gstRateApplied = settings.defaultGstRate,
                pstRateApplied = settings.defaultPstRate,
            )
        } else {
            hourRepository.endShift(current)
        }
    }
}
