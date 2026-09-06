package com.gamilo.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.gamilo.app.data.entity.JobEntity
import com.gamilo.app.data.model.JobStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class JobDaoTest {

    @get:Rule
    val dbRule = DbTestRule()

    private val dao get() = dbRule.database.jobDao()

    private fun job(
        clientName: String = "Acme Co",
        title: String = "Deck rebuild",
        status: JobStatus = JobStatus.ACTIVE,
    ) = JobEntity(
        clientName = clientName,
        title = title,
        status = status,
        notes = "",
        createdAt = 1_000L,
        updatedAt = 1_000L,
        deletedAt = null,
    )

    @Test
    fun insertAndGetById_roundTrips() = runTest {
        val id = dao.insert(job())
        val loaded = dao.getById(id)
        assertEquals("Acme Co", loaded?.clientName)
    }

    @Test
    fun observeActive_excludesOtherStatuses() = runTest {
        dao.insert(job(status = JobStatus.ACTIVE))
        dao.insert(job(status = JobStatus.COMPLETED))
        dao.insert(job(status = JobStatus.CANCELLED))

        assertEquals(1, dao.observeActive().first().size)
    }

    @Test
    fun softDeletedJob_isExcludedFromObserveAll_butVisibleIncludingDeleted() = runTest {
        val id = dao.insert(job())
        dao.softDelete(id, deletedAt = 2_000L)

        assertEquals(0, dao.observeAll().first().size)
        assertTrue(dao.observeAllIncludingDeleted().first().isNotEmpty())
    }

    @Test
    fun getById_returnsNullForUnknownId() = runTest {
        assertNull(dao.getById(999L))
    }
}
