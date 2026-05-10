package dev.bhaarath.hydratemeds.shared.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.bhaarath.hydratemeds.shared.model.ActiveReminder
import dev.bhaarath.hydratemeds.shared.model.ReminderId
import dev.bhaarath.hydratemeds.shared.model.ReminderType
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "active_reminders",
    indices = [
        Index(value = ["scheduled_at_epoch_millis"]),
        Index(value = ["scheduled_local_date"]),
    ],
)
data class ActiveReminderEntity(
    @PrimaryKey
    @ColumnInfo(name = "reminder_id")
    val reminderId: String,
    @ColumnInfo(name = "reminder_type")
    val reminderType: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "scheduled_at_epoch_millis")
    val scheduledAtEpochMillis: Long,
    @ColumnInfo(name = "scheduled_local_date")
    val scheduledLocalDate: String,
    @ColumnInfo(name = "fired_at_epoch_millis")
    val firedAtEpochMillis: Long,
    @ColumnInfo(name = "last_notified_at_epoch_millis")
    val lastNotifiedAtEpochMillis: Long?,
)

fun ActiveReminderEntity.toModel(): ActiveReminder =
    ActiveReminder(
        reminderId = ReminderId(reminderId),
        reminderType = ReminderType.fromWireName(reminderType),
        title = title,
        scheduledAt = Instant.ofEpochMilli(scheduledAtEpochMillis),
        scheduledLocalDate = LocalDate.parse(scheduledLocalDate),
        firedAt = Instant.ofEpochMilli(firedAtEpochMillis),
        lastNotifiedAt = lastNotifiedAtEpochMillis?.let(Instant::ofEpochMilli),
    )

fun ActiveReminder.toEntity(): ActiveReminderEntity =
    ActiveReminderEntity(
        reminderId = reminderId.value,
        reminderType = reminderType.wireName,
        title = title,
        scheduledAtEpochMillis = scheduledAt.toEpochMilli(),
        scheduledLocalDate = scheduledLocalDate.toString(),
        firedAtEpochMillis = firedAt.toEpochMilli(),
        lastNotifiedAtEpochMillis = lastNotifiedAt?.toEpochMilli(),
    )

