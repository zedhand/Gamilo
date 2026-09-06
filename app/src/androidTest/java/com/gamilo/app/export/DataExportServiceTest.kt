package com.gamilo.app.export

import com.gamilo.app.core.SystemClock
import com.gamilo.app.data.DbTestRule
import com.gamilo.app.data.entity.ExpenseEntity
import com.gamilo.app.data.entity.JobEntity
import com.gamilo.app.data.model.JobStatus
import com.gamilo.app.data.repo.AppointmentRepository
import com.gamilo.app.data.repo.AttachmentRepository
import com.gamilo.app.data.repo.ExpenseRepository
import com.gamilo.app.data.repo.HourRepository
import com.gamilo.app.data.repo.JobRepository
import com.gamilo.app.data.repo.MileageRepository
import com.gamilo.app.data.repo.ShippingRepository
import com.gamilo.app.data.repo.TaskRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DataExportServiceTest {

    @get:Rule
    val dbRule = DbTestRule()

    private fun service(): DataExportService {
        val db = dbRule.database
        return DataExportService(
            JobRepository(db.jobDao(), SystemClock),
            TaskRepository(db.taskDao(), SystemClock),
            HourRepository(db.hourDao(), SystemClock),
            ExpenseRepository(db.expenseDao(), SystemClock),
            MileageRepository(db.mileageDao(), SystemClock),
            ShippingRepository(db.shippingDao(), SystemClock),
            AttachmentRepository(db.attachmentDao(), SystemClock),
            AppointmentRepository(db.appointmentDao(), SystemClock),
        )
    }

    @Test
    fun buildCombinedCsv_includesEverySectionHeader() = runTest {
        val csv = service().buildCombinedCsv()
        for (section in listOf("JOBS", "TASKS", "HOURS", "EXPENSES", "MILEAGE", "SHIPPING", "ATTACHMENTS", "APPOINTMENTS")) {
            assertTrue("expected $section section header", csv.contains("$section\r\n"))
        }
    }

    @Test
    fun buildCombinedCsv_includesSoftDeletedRowsWithTheirDeletedAtStamp() = runTest {
        val jobRepo = JobRepository(dbRule.database.jobDao(), SystemClock)
        val expenseRepo = ExpenseRepository(dbRule.database.expenseDao(), SystemClock)

        val jobId = jobRepo.create(
            JobEntity(clientName = "Jane Smith", title = "Replace faucet", status = JobStatus.ACTIVE, notes = "", createdAt = 0, updatedAt = 0, deletedAt = null),
        )
        val expenseId = expenseRepo.create(
            ExpenseEntity(
                jobId = jobId, description = "Faucet cartridge", cost = java.math.BigDecimal("14.50"),
                currencyCode = "CAD", fxRateApplied = java.math.BigDecimal.ONE, costCad = java.math.BigDecimal.ZERO,
                gstRateApplied = java.math.BigDecimal.ZERO, pstRateApplied = java.math.BigDecimal.ZERO,
                photoUri = null, purchasedAt = 1_000L, createdAt = 0, updatedAt = 0, deletedAt = null,
            ),
        )
        expenseRepo.softDelete(expenseId)

        val csv = service().buildCombinedCsv()

        assertTrue("soft-deleted expense row should still be exported", csv.contains("Faucet cartridge"))
        val expenseSection = csv.substringAfter("EXPENSES\r\n").substringBefore("\r\n\r\n")
        val expenseRow = expenseSection.lines().first { it.contains("Faucet cartridge") }
        val deletedAtColumn = expenseRow.split(",").last()
        assertTrue("deleted_at column should be populated, not blank", deletedAtColumn.isNotBlank())
    }
}
