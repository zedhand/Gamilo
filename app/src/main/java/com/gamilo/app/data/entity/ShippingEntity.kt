package com.gamilo.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gamilo.app.data.model.CoverageParty
import com.gamilo.app.data.model.ShippingCarrier
import java.math.BigDecimal

@Entity(
    tableName = "shipping",
    foreignKeys = [
        ForeignKey(
            entity = JobEntity::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("jobId"), Index("deletedAt"), Index("trackingNumber")],
)
data class ShippingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobId: Long?,
    val carrier: ShippingCarrier,
    val trackingNumber: String,
    val shippingCost: BigDecimal,
    val currencyCode: String,
    val fxRateApplied: BigDecimal,
    val shippingCostCad: BigDecimal,
    val insuranceCost: BigDecimal,
    val insuranceCostCad: BigDecimal,
    val declaredValue: BigDecimal,
    val declaredValueCad: BigDecimal,
    val gstRateApplied: BigDecimal,
    val pstRateApplied: BigDecimal,
    /** Whether the business (SELLER) or the CLIENT absorbs shippingCost + insuranceCost. */
    val coverage: CoverageParty,
    val lengthCm: BigDecimal,
    val widthCm: BigDecimal,
    val heightCm: BigDecimal,
    val notes: String,
    val dispatchedAt: Long?,
    val deliveredAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
