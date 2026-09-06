package com.gamilo.app.data.repo

import com.gamilo.app.core.Clock
import com.gamilo.app.core.Money
import com.gamilo.app.data.dao.ShippingDao
import com.gamilo.app.data.entity.ShippingEntity
import kotlinx.coroutines.flow.Flow

class ShippingRepository(private val shippingDao: ShippingDao, private val clock: Clock) {
    fun observeAll(): Flow<List<ShippingEntity>> = shippingDao.observeAll()
    fun observeForJob(jobId: Long): Flow<List<ShippingEntity>> = shippingDao.observeForJob(jobId)
    fun observeAllIncludingDeleted(): Flow<List<ShippingEntity>> = shippingDao.observeAllIncludingDeleted()
    suspend fun getById(id: Long): ShippingEntity? = shippingDao.getById(id)

    private fun freeze(shipment: ShippingEntity) = shipment.copy(
        shippingCostCad = Money.convertToCad(shipment.shippingCost, shipment.fxRateApplied),
        insuranceCostCad = Money.convertToCad(shipment.insuranceCost, shipment.fxRateApplied),
        declaredValueCad = Money.convertToCad(shipment.declaredValue, shipment.fxRateApplied),
    )

    suspend fun create(shipment: ShippingEntity): Long {
        val now = clock.nowMillis()
        return shippingDao.insert(freeze(shipment).copy(createdAt = now, updatedAt = now, deletedAt = null))
    }

    suspend fun update(shipment: ShippingEntity) {
        shippingDao.update(freeze(shipment).copy(updatedAt = clock.nowMillis()))
    }

    suspend fun softDelete(id: Long) = shippingDao.softDelete(id, clock.nowMillis())
}
