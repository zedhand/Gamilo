package com.gamilo.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.gamilo.app.data.entity.JobEntity
import com.gamilo.app.data.entity.TaskEntity
import com.gamilo.app.data.model.JobStatus
import com.gamilo.app.data.model.TaskPriority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class TaskDaoTest {

    @get:Rule
    val dbRule = DbTestRule()

    private val dao get() = dbRule.database.taskDao()

    private fun task(
        jobId: Long? = null,
        title: String = "Pick up lumber",
        priority: TaskPriority = TaskPriority.NORMAL,
        isDone: Boolean = false,
        dueAt: Long? = 5_000L,
    ) = TaskEntity(
        jobId = jobId,
        title = title,
        notes = "",
        priority = priority,
        isDone = isDone,
        doneAt = null,
        dueAt = dueAt,
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
    fun observeForJob_filtersToMatchingJobOnly() = runTest {
        val jobA = insertJob()
        val jobB = insertJob()
        dao.insert(task(jobId = jobA))
        dao.insert(task(jobId = jobB))

        assertEquals(1, dao.observeForJob(jobA).first().size)
    }

    @Test
    fun jobIdIsNullable_forUnassignedTasks() = runTest {
        val id = dao.insert(task(jobId = null))
        assertEquals(null, dao.getById(id)?.jobId)
    }

    @Test
    fun softDeletedTask_isExcludedFromObserveAll() = runTest {
        val id = dao.insert(task())
        dao.softDelete(id, deletedAt = 2_000L)

        assertEquals(0, dao.observeAll().first().size)
        assertTrue(dao.observeAllIncludingDeleted().first().isNotEmpty())
    }
}
