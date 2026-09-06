package com.gamilo.app.ui.screens.hours

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.gamilo.app.core.GlobalFilter
import com.gamilo.app.data.entity.HourEntity
import com.gamilo.app.data.entity.JobEntity
import com.gamilo.app.data.repo.HourRepository
import com.gamilo.app.data.repo.JobRepository
import com.gamilo.app.settings.GamiloSettings
import com.gamilo.app.settings.SettingsStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class HoursViewModel(
    private val hourRepository: HourRepository,
    private val settingsStore: SettingsStore,
    private val jobRepository: JobRepository,
) : ViewModel() {

    private val filterFlow = MutableStateFlow(GlobalFilter())
    fun setFilter(filter: GlobalFilter) { filterFlow.value = filter }

    val entries: Flow<PagingData<HourEntity>> = filterFlow.flatMapLatest { filter ->
        val range = filter.dateRange()
        val (jobId, unassignedOnly) = filter.jobFilter.toQueryParams()
        Pager(PagingConfig(pageSize = 20)) {
            hourRepository.pagingSource(jobId, unassignedOnly, range.startMillis, range.endMillis)
        }.flow
    }.cachedIn(viewModelScope)

    val settings: Flow<GamiloSettings> = settingsStore.settings
    val jobs: Flow<List<JobEntity>> = jobRepository.observeActive()

    /** A manual entry for time already worked — hoursWorked ago through now, at hourlyRate. */
    fun addManualEntry(hoursWorked: BigDecimal, hourlyRate: BigDecimal, jobId: Long?) = viewModelScope.launch {
        val settings = settingsStore.settings.first()
        val now = System.currentTimeMillis()
        val durationMillis = hoursWorked.multiply(BigDecimal(TimeUnit.HOURS.toMillis(1))).toLong()
        hourRepository.create(
            HourEntity(
                jobId = jobId,
                startedAt = now - durationMillis,
                endedAt = now,
                hourlyRate = hourlyRate,
                currencyCode = settings.baseCurrencyCode,
                fxRateApplied = settings.currentFxRateApplied,
                hourlyRateCad = BigDecimal.ZERO,
                gstRateApplied = settings.defaultGstRate,
                pstRateApplied = settings.defaultPstRate,
                notes = "",
                createdAt = 0,
                updatedAt = 0,
                deletedAt = null,
            ),
        )
    }

    fun deleteEntry(id: Long) = viewModelScope.launch { hourRepository.softDelete(id) }
}
