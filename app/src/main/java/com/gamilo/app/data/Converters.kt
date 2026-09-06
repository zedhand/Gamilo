package com.gamilo.app.data

import androidx.room.TypeConverter
import com.gamilo.app.data.model.AttachmentOwnerType
import com.gamilo.app.data.model.CoverageParty
import com.gamilo.app.data.model.JobStatus
import com.gamilo.app.data.model.ShippingCarrier
import com.gamilo.app.data.model.TaskPriority
import java.math.BigDecimal

/**
 * Every money field goes through BigDecimal <-> TEXT, never Float/Double — see
 * core/Money.kt. Enum converters fall back to a documented default on an unrecognized
 * stored name (e.g. a downgrade after a value was renamed) rather than throwing — a
 * corrupt/unknown row must never crash the whole list query.
 */
class Converters {

    @TypeConverter
    fun bigDecimalToString(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun stringToBigDecimal(value: String?): BigDecimal? = value?.let { BigDecimal(it) }

    @TypeConverter fun jobStatusToString(v: JobStatus?): String? = v?.name
    @TypeConverter fun stringToJobStatus(v: String?): JobStatus? =
        v?.let { runCatching { JobStatus.valueOf(it) }.getOrDefault(JobStatus.ACTIVE) }

    @TypeConverter fun taskPriorityToString(v: TaskPriority?): String? = v?.name
    @TypeConverter fun stringToTaskPriority(v: String?): TaskPriority? =
        v?.let { runCatching { TaskPriority.valueOf(it) }.getOrDefault(TaskPriority.NORMAL) }

    @TypeConverter fun shippingCarrierToString(v: ShippingCarrier?): String? = v?.name
    @TypeConverter fun stringToShippingCarrier(v: String?): ShippingCarrier? =
        v?.let { runCatching { ShippingCarrier.valueOf(it) }.getOrDefault(ShippingCarrier.OTHER) }

    @TypeConverter fun coveragePartyToString(v: CoverageParty?): String? = v?.name
    @TypeConverter fun stringToCoverageParty(v: String?): CoverageParty? =
        v?.let { runCatching { CoverageParty.valueOf(it) }.getOrDefault(CoverageParty.SELLER) }

    @TypeConverter fun attachmentOwnerTypeToString(v: AttachmentOwnerType?): String? = v?.name
    @TypeConverter fun stringToAttachmentOwnerType(v: String?): AttachmentOwnerType? =
        v?.let { runCatching { AttachmentOwnerType.valueOf(it) }.getOrDefault(AttachmentOwnerType.JOB) }
}
