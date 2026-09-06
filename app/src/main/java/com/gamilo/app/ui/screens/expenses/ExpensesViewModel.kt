package com.gamilo.app.ui.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.gamilo.app.core.GlobalFilter
import com.gamilo.app.data.entity.AttachmentEntity
import com.gamilo.app.data.entity.ExpenseEntity
import com.gamilo.app.data.entity.JobEntity
import com.gamilo.app.data.model.AttachmentOwnerType
import com.gamilo.app.data.repo.AttachmentRepository
import com.gamilo.app.data.repo.ExpenseRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensesViewModel(
    private val expenseRepository: ExpenseRepository,
    private val attachmentRepository: AttachmentRepository,
    private val settingsStore: SettingsStore,
    private val jobRepository: JobRepository,
) : ViewModel() {

    private val filterFlow = MutableStateFlow(GlobalFilter())
    fun setFilter(filter: GlobalFilter) { filterFlow.value = filter }

    val entries: Flow<PagingData<ExpenseEntity>> = filterFlow.flatMapLatest { filter ->
        val range = filter.dateRange()
        val (jobId, unassignedOnly) = filter.jobFilter.toQueryParams()
        Pager(PagingConfig(pageSize = 20)) {
            expenseRepository.pagingSource(jobId, unassignedOnly, range.startMillis, range.endMillis)
        }.flow
    }.cachedIn(viewModelScope)

    val settings: Flow<GamiloSettings> = settingsStore.settings
    val jobs: Flow<List<JobEntity>> = jobRepository.observeActive()

    fun addExpense(description: String, cost: BigDecimal, photoUri: String?, jobId: Long?) = viewModelScope.launch {
        val settings = settingsStore.settings.first()
        val now = System.currentTimeMillis()
        val id = expenseRepository.create(
            ExpenseEntity(
                jobId = jobId,
                description = description,
                cost = cost,
                currencyCode = settings.baseCurrencyCode,
                fxRateApplied = settings.currentFxRateApplied,
                costCad = BigDecimal.ZERO,
                gstRateApplied = settings.defaultGstRate,
                pstRateApplied = settings.defaultPstRate,
                photoUri = photoUri,
                purchasedAt = now,
                createdAt = 0,
                updatedAt = 0,
                deletedAt = null,
            ),
        )
        if (photoUri != null) {
            attachmentRepository.create(
                AttachmentEntity(
                    ownerType = AttachmentOwnerType.EXPENSE,
                    ownerId = id,
                    uri = photoUri,
                    label = "Receipt",
                    capturedAt = now,
                    createdAt = 0,
                    deletedAt = null,
                ),
            )
        }
    }

    fun deleteExpense(id: Long) = viewModelScope.launch { expenseRepository.softDelete(id) }
}
