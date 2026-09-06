package com.gamilo.app.data.repo

import com.gamilo.app.core.Clock
import com.gamilo.app.data.dao.AttachmentDao
import com.gamilo.app.data.entity.AttachmentEntity
import com.gamilo.app.data.model.AttachmentOwnerType
import kotlinx.coroutines.flow.Flow

class AttachmentRepository(private val attachmentDao: AttachmentDao, private val clock: Clock) {
    fun observeFor(ownerType: AttachmentOwnerType, ownerId: Long): Flow<List<AttachmentEntity>> =
        attachmentDao.observeFor(ownerType, ownerId)

    fun observeAllIncludingDeleted(): Flow<List<AttachmentEntity>> = attachmentDao.observeAllIncludingDeleted()

    suspend fun create(attachment: AttachmentEntity): Long =
        attachmentDao.insert(attachment.copy(createdAt = clock.nowMillis(), deletedAt = null))

    suspend fun softDelete(id: Long) = attachmentDao.softDelete(id, clock.nowMillis())
}
