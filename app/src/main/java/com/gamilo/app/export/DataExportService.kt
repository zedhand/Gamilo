package com.gamilo.app.export

import com.gamilo.app.core.TimeFormat
import com.gamilo.app.data.repo.AppointmentRepository
import com.gamilo.app.data.repo.AttachmentRepository
import com.gamilo.app.data.repo.ExpenseRepository
import com.gamilo.app.data.repo.HourRepository
import com.gamilo.app.data.repo.JobRepository
import com.gamilo.app.data.repo.MileageRepository
import com.gamilo.app.data.repo.ShippingRepository
import com.gamilo.app.data.repo.TaskRepository
import kotlinx.coroutines.flow.first

/**
 * Builds one combined CSV covering every entity, including soft-deleted rows (the audit path —
 * see each repository's observeAllIncludingDeleted) so an exported report always reconciles
 * against historical tax filings even for records since deleted. Every monetary row carries its
 * native amount, currency code, and the fx rate actually applied, per the historical-snapshot
 * rule — never a live-recomputed figure.
 */
class DataExportService(
    private val jobRepository: JobRepository,
    private val taskRepository: TaskRepository,
    private val hourRepository: HourRepository,
    private val expenseRepository: ExpenseRepository,
    private val mileageRepository: MileageRepository,
    private val shippingRepository: ShippingRepository,
    private val attachmentRepository: AttachmentRepository,
    private val appointmentRepository: AppointmentRepository,
) {
    suspend fun buildCombinedCsv(): String {
        val builder = StringBuilder()

        builder.append("JOBS\r\n")
        val jobs = jobRepository.observeAllIncludingDeleted().first()
        builder.append(
            CsvExporter.toCsv(
                listOf("id", "client_name", "title", "status", "notes", "created_at", "updated_at", "deleted_at"),
                jobs.map { j ->
                    listOf(
                        j.id.toString(), j.clientName, j.title, j.status.name, j.notes,
                        TimeFormat.formatDateTime(j.createdAt), TimeFormat.formatDateTime(j.updatedAt),
                        j.deletedAt?.let { TimeFormat.formatDateTime(it) } ?: "",
                    )
                },
            ),
        )

        builder.append("\r\nTASKS\r\n")
        val tasks = taskRepository.observeAllIncludingDeleted().first()
        builder.append(
            CsvExporter.toCsv(
                listOf("id", "job_id", "title", "notes", "priority", "is_done", "due_at", "created_at", "deleted_at"),
                tasks.map { t ->
                    listOf(
                        t.id.toString(), t.jobId?.toString() ?: "", t.title, t.notes, t.priority.name,
                        t.isDone.toString(), t.dueAt?.let { TimeFormat.formatDateTime(it) } ?: "",
                        TimeFormat.formatDateTime(t.createdAt), t.deletedAt?.let { TimeFormat.formatDateTime(it) } ?: "",
                    )
                },
            ),
        )

        builder.append("\r\nHOURS\r\n")
        val hours = hourRepository.observeAllIncludingDeleted().first()
        builder.append(
            CsvExporter.toCsv(
                listOf(
                    "id", "job_id", "started_at", "ended_at", "hourly_rate", "currency",
                    "fx_rate_applied", "hourly_rate_cad", "notes", "deleted_at",
                ),
                hours.map { h ->
                    listOf(
                        h.id.toString(), h.jobId?.toString() ?: "", TimeFormat.formatDateTime(h.startedAt),
                        h.endedAt?.let { TimeFormat.formatDateTime(it) } ?: "RUNNING",
                        h.hourlyRate.toPlainString(), h.currencyCode, h.fxRateApplied.toPlainString(),
                        h.hourlyRateCad.toPlainString(), h.notes,
                        h.deletedAt?.let { TimeFormat.formatDateTime(it) } ?: "",
                    )
                },
            ),
        )

        builder.append("\r\nEXPENSES\r\n")
        val expenses = expenseRepository.observeAllIncludingDeleted().first()
        builder.append(
            CsvExporter.toCsv(
                listOf(
                    "id", "job_id", "description", "cost", "currency", "fx_rate_applied",
                    "cost_cad", "purchased_at", "deleted_at",
                ),
                expenses.map { e ->
                    listOf(
                        e.id.toString(), e.jobId?.toString() ?: "", e.description, e.cost.toPlainString(),
                        e.currencyCode, e.fxRateApplied.toPlainString(), e.costCad.toPlainString(),
                        TimeFormat.formatDateTime(e.purchasedAt), e.deletedAt?.let { TimeFormat.formatDateTime(it) } ?: "",
                    )
                },
            ),
        )

        builder.append("\r\nMILEAGE\r\n")
        val trips = mileageRepository.observeAllIncludingDeleted().first()
        builder.append(
            CsvExporter.toCsv(
                listOf(
                    "id", "job_id", "occurred_at", "origin", "destination", "distance_km",
                    "mileage_rate_applied", "currency", "fx_rate_applied", "amount_cad", "notes", "deleted_at",
                ),
                trips.map { m ->
                    listOf(
                        m.id.toString(), m.jobId?.toString() ?: "", TimeFormat.formatDateTime(m.occurredAt),
                        m.originLabel, m.destinationLabel, m.distanceKm.toPlainString(),
                        m.mileageRateApplied.toPlainString(), m.currencyCode, m.fxRateApplied.toPlainString(),
                        m.amountCad.toPlainString(), m.notes,
                        m.deletedAt?.let { TimeFormat.formatDateTime(it) } ?: "",
                    )
                },
            ),
        )

        builder.append("\r\nSHIPPING\r\n")
        val shipments = shippingRepository.observeAllIncludingDeleted().first()
        builder.append(
            CsvExporter.toCsv(
                listOf(
                    "id", "job_id", "carrier", "tracking_number", "shipping_cost", "currency",
                    "fx_rate_applied", "shipping_cost_cad", "insurance_cost", "insurance_cost_cad",
                    "declared_value", "declared_value_cad", "coverage", "dispatched_at", "delivered_at", "deleted_at",
                ),
                shipments.map { s ->
                    listOf(
                        s.id.toString(), s.jobId?.toString() ?: "", s.carrier.name, s.trackingNumber,
                        s.shippingCost.toPlainString(), s.currencyCode, s.fxRateApplied.toPlainString(),
                        s.shippingCostCad.toPlainString(), s.insuranceCost.toPlainString(), s.insuranceCostCad.toPlainString(),
                        s.declaredValue.toPlainString(), s.declaredValueCad.toPlainString(), s.coverage.name,
                        s.dispatchedAt?.let { TimeFormat.formatDateTime(it) } ?: "",
                        s.deliveredAt?.let { TimeFormat.formatDateTime(it) } ?: "",
                        s.deletedAt?.let { TimeFormat.formatDateTime(it) } ?: "",
                    )
                },
            ),
        )

        builder.append("\r\nAPPOINTMENTS\r\n")
        val appointments = appointmentRepository.observeAllIncludingDeleted().first()
        builder.append(
            CsvExporter.toCsv(
                listOf("id", "job_id", "title", "start_at", "end_at", "location", "notes", "deleted_at"),
                appointments.map { a ->
                    listOf(
                        a.id.toString(), a.jobId?.toString() ?: "", a.title,
                        TimeFormat.formatDateTime(a.startAt), TimeFormat.formatDateTime(a.endAt),
                        a.location, a.notes, a.deletedAt?.let { TimeFormat.formatDateTime(it) } ?: "",
                    )
                },
            ),
        )

        builder.append("\r\nATTACHMENTS\r\n")
        val attachments = attachmentRepository.observeAllIncludingDeleted().first()
        builder.append(
            CsvExporter.toCsv(
                listOf("id", "owner_type", "owner_id", "uri", "label", "captured_at", "deleted_at"),
                attachments.map { a ->
                    listOf(
                        a.id.toString(), a.ownerType.name, a.ownerId.toString(), a.uri, a.label,
                        TimeFormat.formatDateTime(a.capturedAt), a.deletedAt?.let { TimeFormat.formatDateTime(it) } ?: "",
                    )
                },
            ),
        )

        return builder.toString()
    }
}
