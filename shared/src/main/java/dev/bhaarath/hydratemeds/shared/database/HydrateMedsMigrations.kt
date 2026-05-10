package dev.bhaarath.hydratemeds.shared.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object HydrateMedsMigrations {
    val migration1To2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS daily_reminder_pauses (
                    local_date TEXT NOT NULL,
                    reminder_type TEXT NOT NULL,
                    created_at_epoch_millis INTEGER NOT NULL,
                    PRIMARY KEY(local_date, reminder_type)
                )
                """.trimIndent(),
            )
        }
    }
}
