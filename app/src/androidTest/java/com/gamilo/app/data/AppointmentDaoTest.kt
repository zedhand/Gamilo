package com.gamilo.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.gamilo.app.data.entity.AppointmentEntity
import com.gamilo.app.data.entity.JobEntity
import com.gamilo.app.data.model.JobStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class AppointmentDaoTest {

    @get:Rule
    val dbRule = DbTestRule()

    private val dao get() = dbRule.database.appointmentDao()

    private fun appointment(jobId: Long? = null, startAt: Long = 10_000L, endAt: Long = 13_600_000L) = AppointmentEntity(
        jobId = jobId,
        title = "Site visit",
        startAt = startAt,
        endAt = endAt,
        location = "123 Main St",
        notes = "",
        createdAt = 1_000L,
        updatedAt = 1_000L,
        deletedAt = null,
    )

    private suspend fun insertJob(): Long = dbRule.database.jobDao().insert(
        JobEntity(clientName = "Jane Smith", title = "Replace faucet", status = JobStatus.ACTIVE, notes = "", createdAt = 1_000L, updatedAt = 1_000L, deletedAt = null),
    )

    @Test
    fun observeAll_ordersByStartAtAscending() = runTest {
        dao.insert(appointment(startAt = 20_000L))
        dao.insert(appointment(startAt = 10_000L))

        val results = dao.observeAll().first()
        assertEquals(10_000L, results[0].startAt)
        assertEquals(20_000L, results[1].startAt)
    }

    @Test
    fun observeInRange_excludesAppointmentsOutsideTheWindow() = runTest {
        dao.insert(appointment(startAt = 5_000L))
        dao.insert(appointment(startAt = 15_000L))
        dao.insert(appointment(startAt = 25_000L))

        val inRange = dao.observeInRange(10_000L, 20_000L).first()
        assertEquals(1, inRange.size)
        assertEquals(15_000L, inRange[0].startAt)
    }

    @Test
    fun jobIdRoundTrips_andSurvivesJobLookup() = runTest {
        val jobId = insertJob()
        val id = dao.insert(appointment(jobId = jobId))
        assertEquals(jobId, dao.getById(id)?.jobId)
    }

    @Test
    fun softDeletedAppointment_isExcludedFromObserveAllButNotFromAudit() = runTest {
        val id = dao.insert(appointment())
        dao.softDelete(id, deletedAt = 2_000L)

        assertEquals(0, dao.observeAll().first().size)
        assertTrue(dao.observeAllIncludingDeleted().first().isNotEmpty())
    }
}
