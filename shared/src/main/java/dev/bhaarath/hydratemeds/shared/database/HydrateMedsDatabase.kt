package dev.bhaarath.hydratemeds.shared.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AcknowledgmentLogEntity::class,
        ActiveReminderEntity::class,
        DailyReminderPauseEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class HydrateMedsDatabase : RoomDatabase() {
    abstract fun acknowledgmentLogDao(): AcknowledgmentLogDao
    abstract fun activeReminderDao(): ActiveReminderDao
    abstract fun dailyReminderPauseDao(): DailyReminderPauseDao
}
