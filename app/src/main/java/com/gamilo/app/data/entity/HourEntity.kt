package com.gamilo.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * A logged work session. [endedAt] null means the session is still running — this is also
 * what powers the Home tab's [Start Shift / End Shift] button: starting a shift is inserting
 * a row with [endedAt] = null, ending it is filling that field in. At most one such row
 * should exist un-deleted at a time (enforced by the repository, not the schema).
 */
@Entity(
    tableName = "hours",
    foreignKeys = [
        ForeignKey(
            entity = JobEntity::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("jobId"), Index("startedAt"), Index("endedAt"), Index("deletedAt")],
)
data class HourEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobId: Long?,
    val startedAt: Long,
    val endedAt: Long?,
    val hourlyRate: BigDecimal,
    val currencyCode: String,
    val fxRateApplied: BigDecimal,
    /** hourlyRate converted to CAD at write time — frozen, never recomputed. */
    val hourlyRateCad: BigDecimal,
    val gstRateApplied: BigDecimal,
    val pstRateApplied: BigDecimal,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
