package com.gamilo.app.ui.screens.shipping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamilo.app.core.GlobalFilter
import com.gamilo.app.data.entity.JobEntity
import com.gamilo.app.data.entity.ShippingEntity
import com.gamilo.app.data.model.CoverageParty
import com.gamilo.app.data.model.ShippingCarrier
import com.gamilo.app.data.repo.JobRepository
import com.gamilo.app.data.repo.ShippingRepository
import com.gamilo.app.settings.SettingsStore
import com.gamilo.app.shipping.ParsedLabel
import com.gamilo.app.shipping.ShippingLabelParser
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal

class ShippingViewModel(
    private val shippingRepository: ShippingRepository,
    private val settingsStore: SettingsStore,
    private val jobRepository: JobRepository,
) : ViewModel() {

    private val filterFlow = MutableStateFlow(GlobalFilter())
    fun setFilter(filter: GlobalFilter) { filterFlow.value = filter }

    /**
     * Date-range filtering uses [ShippingEntity.createdAt] (always populated) rather than the
     * nullable dispatch/delivery timestamps, so a shipment created today still shows under
     * "Today" even before it's actually been dispatched.
     */
    val entries: Flow<List<ShippingEntity>> = combine(shippingRepository.observeAll(), filterFlow) { shipments, filter ->
        val range = filter.dateRange()
        shipments.filter { s -> filter.jobFilter.matches(s.jobId) && s.createdAt in range }
    }
    val jobs: Flow<List<JobEntity>> = jobRepository.observeActive()

    suspend fun scanLabel(image: InputImage): ParsedLabel = ShippingLabelParser.parse(image)

    fun addShipment(
        carrier: ShippingCarrier,
        trackingNumber: String,
        shippingCost: BigDecimal,
        insuranceCost: BigDecimal,
        declaredValue: BigDecimal,
        lengthCm: BigDecimal,
        widthCm: BigDecimal,
        heightCm: BigDecimal,
        coverage: CoverageParty,
        jobId: Long?,
    ) = viewModelScope.launch {
        val settings = settingsStore.settings.first()
        shippingRepository.create(
            ShippingEntity(
                jobId = jobId,
                carrier = carrier,
                trackingNumber = trackingNumber,
                shippingCost = shippingCost,
                currencyCode = settings.baseCurrencyCode,
                fxRateApplied = settings.currentFxRateApplied,
                shippingCostCad = BigDecimal.ZERO,
                insuranceCost = insuranceCost,
                insuranceCostCad = BigDecimal.ZERO,
                declaredValue = declaredValue,
                declaredValueCad = BigDecimal.ZERO,
                gstRateApplied = settings.defaultGstRate,
                pstRateApplied = settings.defaultPstRate,
                coverage = coverage,
                lengthCm = lengthCm,
                widthCm = widthCm,
                heightCm = heightCm,
                notes = "",
                dispatchedAt = null,
                deliveredAt = null,
                createdAt = 0,
                updatedAt = 0,
                deletedAt = null,
            ),
        )
    }

    fun deleteShipment(id: Long) = viewModelScope.launch { shippingRepository.softDelete(id) }
}
