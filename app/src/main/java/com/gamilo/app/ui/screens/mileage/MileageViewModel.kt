package com.gamilo.app.ui.screens.mileage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.gamilo.app.core.GlobalFilter
import com.gamilo.app.data.entity.JobEntity
import com.gamilo.app.data.entity.MileageEntity
import com.gamilo.app.data.repo.JobRepository
import com.gamilo.app.data.repo.MileageRepository
import com.gamilo.app.settings.GamiloSettings
import com.gamilo.app.settings.SettingsStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class MileageViewModel(
    private val mileageRepository: MileageRepository,
    private val settingsStore: SettingsStore,
    private val jobRepository: JobRepository,
) : ViewModel() {

    private val filterFlow = MutableStateFlow(GlobalFilter())
    fun setFilter(filter: GlobalFilter) { filterFlow.value = filter }

    val entries: Flow<PagingData<MileageEntity>> = filterFlow.flatMapLatest { filter ->
        val range = filter.dateRange()
        val (jobId, unassignedOnly) = filter.jobFilter.toQueryParams()
        Pager(PagingConfig(pageSize = 20)) {
            mileageRepository.pagingSource(jobId, unassignedOnly, range.startMillis, range.endMillis)
        }.flow
    }.cachedIn(viewModelScope)

    val settings: Flow<GamiloSettings> = settingsStore.settings
    val jobs: Flow<List<JobEntity>> = jobRepository.observeActive()

    fun addTrip(origin: String, destination: String, distanceKm: BigDecimal, jobId: Long?) = viewModelScope.launch {
        val settings = settingsStore.settings.first()
        mileageRepository.create(
            MileageEntity(
                jobId = jobId,
                occurredAt = System.currentTimeMillis(),
                originLabel = origin,
                destinationLabel = destination,
                distanceKm = distanceKm,
                mileageRateApplied = settings.defaultMileageRatePerKm,
                currencyCode = settings.baseCurrencyCode,
                fxRateApplied = settings.currentFxRateApplied,
                amountCad = BigDecimal.ZERO,
                notes = "",
                createdAt = 0,
                updatedAt = 0,
                deletedAt = null,
            ),
        )
    }

    fun deleteTrip(id: Long) = viewModelScope.launch { mileageRepository.softDelete(id) }
}
