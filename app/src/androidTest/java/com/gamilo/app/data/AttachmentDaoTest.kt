package com.gamilo.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.gamilo.app.data.entity.AttachmentEntity
import com.gamilo.app.data.model.AttachmentOwnerType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class AttachmentDaoTest {

    @get:Rule
    val dbRule = DbTestRule()

    private val dao get() = dbRule.database.attachmentDao()

    private fun attachment(ownerType: AttachmentOwnerType = AttachmentOwnerType.EXPENSE, ownerId: Long = 1L) =
        AttachmentEntity(
            ownerType = ownerType,
            ownerId = ownerId,
            uri = "content://gamilo/receipts/1",
            label = "Receipt",
            capturedAt = 1_000L,
            createdAt = 1_000L,
            deletedAt = null,
        )

    @Test
    fun observeFor_filtersByOwnerTypeAndId() = runTest {
        dao.insert(attachment(ownerType = AttachmentOwnerType.EXPENSE, ownerId = 1L))
        dao.insert(attachment(ownerType = AttachmentOwnerType.EXPENSE, ownerId = 2L))
        dao.insert(attachment(ownerType = AttachmentOwnerType.SHIPPING, ownerId = 1L))

        assertEquals(1, dao.observeFor(AttachmentOwnerType.EXPENSE, 1L).first().size)
    }

    @Test
    fun softDeletedAttachment_isExcludedFromObserveFor() = runTest {
        val id = dao.insert(attachment())
        dao.softDelete(id, deletedAt = 2_000L)

        assertEquals(0, dao.observeFor(AttachmentOwnerType.EXPENSE, 1L).first().size)
    }
}
