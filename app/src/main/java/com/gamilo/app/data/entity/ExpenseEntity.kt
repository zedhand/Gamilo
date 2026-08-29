package com.gamilo.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

/** Any job-related expense — materials, tools, permits, fuel, etc. — not just building materials. */
@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = JobEntity::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("jobId"), Index("purchasedAt"), Index("deletedAt")],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobId: Long?,
    val description: String,
    val cost: BigDecimal,
    val currencyCode: String,
    val fxRateApplied: BigDecimal,
    /** cost converted to CAD at write time — frozen, never recomputed. */
    val costCad: BigDecimal,
    val gstRateApplied: BigDecimal,
    val pstRateApplied: BigDecimal,
    /** content:// URI of the receipt photo attachment, if one was captured. */
    val photoUri: String?,
    val purchasedAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
