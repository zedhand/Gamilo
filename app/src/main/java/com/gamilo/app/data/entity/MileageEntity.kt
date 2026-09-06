package com.gamilo.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * A per-km reimbursement rate is a deemed, non-taxable CRA allowance for a self-employed
 * driver, not a taxable supply — this entity deliberately carries no GST/PST fields, unlike
 * Hours/Materials/Shipping. currencyCode/fxRateApplied are still stored on every row (Gamilo
 * mandates this for all transactional entities), even though mileage is CAD-only in practice.
 */
@Entity(
    tableName = "mileage",
    foreignKeys = [
        ForeignKey(
            entity = JobEntity::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("jobId"), Index("occurredAt"), Index("deletedAt")],
)
data class MileageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobId: Long?,
    val occurredAt: Long,
    val originLabel: String,
    val destinationLabel: String,
    val distanceKm: BigDecimal,
    /** Rate per km, snapshotted so a later CRA rate change never reprices this row. */
    val mileageRateApplied: BigDecimal,
    val currencyCode: String,
    val fxRateApplied: BigDecimal,
    /** distanceKm * mileageRateApplied * fxRateApplied, computed and frozen at write time. */
    val amountCad: BigDecimal,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
