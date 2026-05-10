package dev.bhaarath.hydratemeds.shared.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import dev.bhaarath.hydratemeds.shared.model.DailyReminderPause
import dev.bhaarath.hydratemeds.shared.model.ReminderType
import java.time.LocalDate

@Entity(
    tableName = "daily_reminder_pauses",
    primaryKeys = ["local_date", "reminder_type"],
)
data class DailyReminderPauseEntity(
    @ColumnInfo(name = "local_date")
    val localDate: String,
    @ColumnInfo(name = "reminder_type")
    val reminderType: String,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
)

fun DailyReminderPauseEntity.toModel(): DailyReminderPause =
    DailyReminderPause(
        date = LocalDate.parse(localDate),
        reminderType = ReminderType.fromWireName(reminderType),
        paused = true,
    )

fun DailyReminderPause.toEntity(nowEpochMillis: Long): DailyReminderPauseEntity =
    DailyReminderPauseEntity(
        localDate = date.toString(),
        reminderType = reminderType.wireName,
        createdAtEpochMillis = nowEpochMillis,
    )
