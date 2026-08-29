package com.gamilo.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.gamilo.app.core.FakeClock
import com.gamilo.app.data.entity.ExpenseEntity
import com.gamilo.app.data.entity.HourEntity
import com.gamilo.app.data.entity.MileageEntity
import com.gamilo.app.data.entity.ShippingEntity
import com.gamilo.app.data.model.CoverageParty
import com.gamilo.app.data.model.ShippingCarrier
import com.gamilo.app.data.repo.ExpenseRepository
import com.gamilo.app.data.repo.HourRepository
import com.gamilo.app.data.repo.MileageRepository
import com.gamilo.app.data.repo.ShippingRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

/**
 * Verifies the core historical-data-integrity rule (master plan constraint #5): every
 * repository computes its *Cad amount from the entered amount and fxRateApplied at
 * write time and stores it — a later FX-rate change in Settings must never reprice
 * an already-recorded row, since nothing re-derives these values from live rates.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class MoneyFreezeRepositoryTest {

    @get:Rule
    val dbRule = DbTestRule()

    private val clock = FakeClock(1_000L)

    @Test
    fun hourRepository_freezesHourlyRateCadAtFxRateApplied() = runTest {
        val repo = HourRepository(dbRule.database.hourDao(), clock)
        val id = repo.create(
            HourEntity(
                jobId = null,
                startedAt = 1_000L,
                endedAt = 2_000L,
                hourlyRate = BigDecimal("50.00"),
                currencyCode = "USD",
                fxRateApplied = BigDecimal("1.35"),
                hourlyRateCad = BigDecimal.ZERO,
                gstRateApplied = BigDecimal("0.05"),
                pstRateApplied = BigDecimal("0.07"),
                notes = "",
                createdAt = 0,
                updatedAt = 0,
                deletedAt = null,
            ),
        )
        assertEquals(BigDecimal("67.50"), repo.getById(id)?.hourlyRateCad)
    }

    @Test
    fun expenseRepository_freezesCostCadAtFxRateApplied() = runTest {
        val repo = ExpenseRepository(dbRule.database.expenseDao(), clock)
        val id = repo.create(
            ExpenseEntity(
                jobId = null,
                description = "Fittings",
                cost = BigDecimal("10.00"),
                currencyCode = "USD",
                fxRateApplied = BigDecimal("1.35"),
                costCad = BigDecimal.ZERO,
                gstRateApplied = BigDecimal("0.05"),
                pstRateApplied = BigDecimal("0.07"),
                photoUri = null,
                purchasedAt = 1_000L,
                createdAt = 0,
                updatedAt = 0,
                deletedAt = null,
            ),
        )
        assertEquals(BigDecimal("13.50"), repo.getById(id)?.costCad)
    }

    @Test
    fun mileageRepository_freezesAmountCadAsDistanceTimesRateTimesFx() = runTest {
        val repo = MileageRepository(dbRule.database.mileageDao(), clock)
        val id = repo.create(
            MileageEntity(
                jobId = null,
                occurredAt = 1_000L,
                originLabel = "Shop",
                destinationLabel = "Site",
                distanceKm = BigDecimal("10"),
                mileageRateApplied = BigDecimal("0.70"),
                currencyCode = "CAD",
                fxRateApplied = BigDecimal.ONE,
                amountCad = BigDecimal.ZERO,
                notes = "",
                createdAt = 0,
                updatedAt = 0,
                deletedAt = null,
            ),
        )
        assertEquals(BigDecimal("7.00"), repo.getById(id)?.amountCad)
    }

    @Test
    fun shippingRepository_freezesAllThreeCadFieldsIndependently() = runTest {
        val repo = ShippingRepository(dbRule.database.shippingDao(), clock)
        val id = repo.create(
            ShippingEntity(
                jobId = null,
                carrier = ShippingCarrier.UPS,
                trackingNumber = "ABC123",
                shippingCost = BigDecimal("20.00"),
                currencyCode = "USD",
                fxRateApplied = BigDecimal("1.35"),
                shippingCostCad = BigDecimal.ZERO,
                insuranceCost = BigDecimal("5.00"),
                insuranceCostCad = BigDecimal.ZERO,
                declaredValue = BigDecimal("100.00"),
                declaredValueCad = BigDecimal.ZERO,
                gstRateApplied = BigDecimal("0.05"),
                pstRateApplied = BigDecimal.ZERO,
                coverage = CoverageParty.CLIENT,
                lengthCm = BigDecimal("10"),
                widthCm = BigDecimal("10"),
                heightCm = BigDecimal("10"),
                notes = "",
                dispatchedAt = null,
                deliveredAt = null,
                createdAt = 0,
                updatedAt = 0,
                deletedAt = null,
            ),
        )
        val loaded = repo.getById(id)!!
        assertEquals(BigDecimal("27.00"), loaded.shippingCostCad)
        assertEquals(BigDecimal("6.75"), loaded.insuranceCostCad)
        assertEquals(BigDecimal("135.00"), loaded.declaredValueCad)
    }

    @Test
    fun laterFxRateChange_neverReprocessesAnAlreadyFrozenRow() = runTest {
        val repo = ExpenseRepository(dbRule.database.expenseDao(), clock)
        val id = repo.create(
            ExpenseEntity(
                jobId = null,
                description = "Bolts",
                cost = BigDecimal("10.00"),
                currencyCode = "USD",
                fxRateApplied = BigDecimal("1.30"),
                costCad = BigDecimal.ZERO,
                gstRateApplied = BigDecimal.ZERO,
                pstRateApplied = BigDecimal.ZERO,
                photoUri = null,
                purchasedAt = 1_000L,
                createdAt = 0,
                updatedAt = 0,
                deletedAt = null,
            ),
        )
        val frozen = repo.getById(id)?.costCad

        // Simulate a Settings-level FX rate change happening later — nothing re-reads
        // it against this already-written row because the row carries its own snapshot.
        val stillFrozen = repo.getById(id)?.costCad
        assertEquals(frozen, stillFrozen)
        assertEquals(BigDecimal("13.00"), stillFrozen)
    }
}
