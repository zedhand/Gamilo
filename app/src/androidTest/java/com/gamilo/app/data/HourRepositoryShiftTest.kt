package com.gamilo.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.gamilo.app.core.FakeClock
import com.gamilo.app.data.repo.HourRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

/** Covers the Home tab's [Start Shift / End Shift] button semantics — see HourEntity's doc. */
@RunWith(AndroidJUnit4::class)
@SmallTest
class HourRepositoryShiftTest {

    @get:Rule
    val dbRule = DbTestRule()

    private val clock = FakeClock(1_000L)
    private val repo get() = HourRepository(dbRule.database.hourDao(), clock)

    @Test
    fun startShift_createsRowWithNullEndedAt() = runTest {
        repo.startShift(null, BigDecimal("45.00"), "CAD", BigDecimal.ONE, BigDecimal("0.05"), BigDecimal("0.07"))
        assertNotNull(repo.getOpenSession())
        assertNull(repo.getOpenSession()?.endedAt)
    }

    @Test
    fun startShift_throwsWhenAShiftIsAlreadyRunning() = runTest {
        repo.startShift(null, BigDecimal("45.00"), "CAD", BigDecimal.ONE, BigDecimal("0.05"), BigDecimal("0.07"))
        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking {
                repo.startShift(null, BigDecimal("45.00"), "CAD", BigDecimal.ONE, BigDecimal("0.05"), BigDecimal("0.07"))
            }
        }
    }

    @Test
    fun endShift_setsEndedAtAndClearsOpenSession() = runTest {
        repo.startShift(null, BigDecimal("45.00"), "CAD", BigDecimal.ONE, BigDecimal("0.05"), BigDecimal("0.07"))
        val open = repo.getOpenSession()!!
        clock.advanceBy(3_600_000L)
        repo.endShift(open)

        assertNull(repo.getOpenSession())
        assertEquals(clock.nowMillis(), repo.getById(open.id)?.endedAt)
    }

    @Test
    fun endShift_throwsIfSessionAlreadyEnded() = runTest {
        repo.startShift(null, BigDecimal("45.00"), "CAD", BigDecimal.ONE, BigDecimal("0.05"), BigDecimal("0.07"))
        val open = repo.getOpenSession()!!
        repo.endShift(open)
        val alreadyEnded = repo.getById(open.id)!!

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { repo.endShift(alreadyEnded) }
        }
    }
}
