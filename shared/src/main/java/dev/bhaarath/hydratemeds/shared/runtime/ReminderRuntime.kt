package dev.bhaarath.hydratemeds.shared.runtime

import android.content.Context
import android.content.pm.PackageManager
import androidx.room.Room
import dev.bhaarath.hydratemeds.shared.database.HydrateMedsDatabase
import dev.bhaarath.hydratemeds.shared.database.HydrateMedsMigrations
import dev.bhaarath.hydratemeds.shared.repository.RoomReminderRepository

object ReminderRuntime {
    @Volatile
    private var database: HydrateMedsDatabase? = null

    fun database(context: Context): HydrateMedsDatabase =
        database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                context.applicationContext,
                HydrateMedsDatabase::class.java,
                "hydrate_meds.db",
            ).addMigrations(HydrateMedsMigrations.migration1To2).build().also { database = it }
        }

    fun repository(context: Context): RoomReminderRepository {
        val db = database(context)
        return RoomReminderRepository(
            acknowledgmentLogDao = db.acknowledgmentLogDao(),
            activeReminderDao = db.activeReminderDao(),
            dailyReminderPauseDao = db.dailyReminderPauseDao(),
        )
    }

    fun isWatch(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)
}
