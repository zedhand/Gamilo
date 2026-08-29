package com.gamilo.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.gamilo.app.data.entity.JobEntity
import com.gamilo.app.data.entity.ShippingEntity
import com.gamilo.app.data.model.CoverageParty
import com.gamilo.app.data.model.JobStatus
import com.gamilo.app.data.model.ShippingCarrier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
@SmallTest
class ShippingDaoTest {

    @get:Rule
    val dbRule = DbTestRule()

    private val dao get() = dbRule.database.shippingDao()

    private fun shipment(jobId: Long? = null, carrier: ShippingCarrier = ShippingCarrier.CANADA_POST) = ShippingEntity(
        jobId = jobId,
        carrier = carrier,
        trackingNumber = "1234567890",
        shippingCost = BigDecimal("22.50"),
        currencyCode = "CAD",
        fxRateApplied = BigDecimal.ONE,
        shippingCostCad = BigDecimal("22.50"),
        insuranceCost = BigDecimal("5.00"),
        insuranceCostCad = BigDecimal("5.00"),
        declaredValue = BigDecimal("150.00"),
        declaredValueCad = BigDecimal("150.00"),
        gstRateApplied = BigDecimal("0.05"),
        pstRateApplied = BigDecimal.ZERO,
        coverage = CoverageParty.SELLER,
        lengthCm = BigDecimal("30"),
        widthCm = BigDecimal("20"),
        heightCm = BigDecimal("15"),
        notes = "",
        dispatchedAt = null,
        deliveredAt = null,
        createdAt = 1_000L,
        updatedAt = 1_000L,
        deletedAt = null,
    )

    private suspend fun insertJob(): Long = dbRule.database.jobDao().insert(
        JobEntity(
            clientName = "Acme Co",
            title = "Deck rebuild",
            status = JobStatus.ACTIVE,
            notes = "",
            createdAt = 1_000L,
            updatedAt = 1_000L,
            deletedAt = null,
        ),
    )

    @Test
    fun observeForJob_filtersCorrectly() = runTest {
        val jobA = insertJob()
        val jobB = insertJob()
        dao.insert(shipment(jobId = jobA))
        dao.insert(shipment(jobId = jobB))

        assertEquals(1, dao.observeForJob(jobA).first().size)
    }

    @Test
    fun carrierEnum_roundTripsThroughConverter() = runTest {
        val id = dao.insert(shipment(carrier = ShippingCarrier.DHL))
        assertEquals(ShippingCarrier.DHL, dao.getById(id)?.carrier)
    }

    @Test
    fun softDeletedShipment_isExcludedFromObserveAll() = runTest {
        val id = dao.insert(shipment())
        dao.softDelete(id, deletedAt = 2_000L)

        assertEquals(0, dao.observeAll().first().size)
        assertTrue(dao.observeAllIncludingDeleted().first().isNotEmpty())
    }
}
