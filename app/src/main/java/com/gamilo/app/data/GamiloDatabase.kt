package com.gamilo.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gamilo.app.data.dao.AppointmentDao
import com.gamilo.app.data.dao.AttachmentDao
import com.gamilo.app.data.dao.ExpenseDao
import com.gamilo.app.data.dao.HourDao
import com.gamilo.app.data.dao.JobDao
import com.gamilo.app.data.dao.MileageDao
import com.gamilo.app.data.dao.ShippingDao
import com.gamilo.app.data.dao.TaskDao
import com.gamilo.app.data.entity.AppointmentEntity
import com.gamilo.app.data.entity.AttachmentEntity
import com.gamilo.app.data.entity.ExpenseEntity
import com.gamilo.app.data.entity.HourEntity
import com.gamilo.app.data.entity.JobEntity
import com.gamilo.app.data.entity.MileageEntity
import com.gamilo.app.data.entity.ShippingEntity
import com.gamilo.app.data.entity.TaskEntity

@Database(
    entities = [
        JobEntity::class,
        TaskEntity::class,
        HourEntity::class,
        ExpenseEntity::class,
        MileageEntity::class,
        ShippingEntity::class,
        AttachmentEntity::class,
        AppointmentEntity::class,
    ],
    // v2 added AppointmentEntity (Phase 5). No Migration is registered — AppContainer opens
    // this with fallbackToDestructiveMigration() since the app has never shipped a release
    // (Stage 6 is still pending), so there's no real user data anywhere to preserve yet.
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class GamiloDatabase : RoomDatabase() {
    abstract fun jobDao(): JobDao
    abstract fun taskDao(): TaskDao
    abstract fun hourDao(): HourDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun mileageDao(): MileageDao
    abstract fun shippingDao(): ShippingDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun appointmentDao(): AppointmentDao

    companion object {
        const val DATABASE_NAME = "gamilo.db"
    }
}
