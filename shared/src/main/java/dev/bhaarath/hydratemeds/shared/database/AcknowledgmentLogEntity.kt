package dev.bhaarath.hydratemeds.shared.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.bhaarath.hydratemeds.shared.model.AcknowledgmentLog
import dev.bhaarath.hydratemeds.shared.model.AcknowledgmentSource
import dev.bhaarath.hydratemeds.shared.model.ReminderId
import dev.bhaarath.hydratemeds.shared.model.ReminderType
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "acknowledgment_logs",
    indices = [
        Index(value = ["scheduled_local_date"]),
        Index(value = ["reminder_type", "scheduled_at_epoch_millis"]),
    ],
)
data class AcknowledgmentLogEntity(
    @PrimaryKey
    @ColumnInfo(name = "reminder_id")
    val reminderId: String,
    @ColumnInfo(name = "reminder_type")
    val reminderType: String,
    @ColumnInfo(name = "scheduled_at_epoch_millis")
    val scheduledAtEpochMillis: Long,
    @ColumnInfo(name = "scheduled_local_date")
    val scheduledLocalDate: String,
    @ColumnInfo(name = "acknowledged_at_epoch_millis")
    val acknowledgedAtEpochMillis: Long,
    @ColumnInfo(name = "source")
    val source: String,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
)

fun AcknowledgmentLogEntity.toModel(): AcknowledgmentLog =
    AcknowledgmentLog(
        reminderId = ReminderId(reminderId),
        reminderType = ReminderType.fromWireName(reminderType),
        scheduledAt = Instant.ofEpochMilli(scheduledAtEpochMillis),
        scheduledLocalDate = LocalDate.parse(scheduledLocalDate),
        acknowledgedAt = Instant.ofEpochMilli(acknowledgedAtEpochMillis),
        source = AcknowledgmentSource.fromWireName(source),
    )

fun AcknowledgmentLog.toEntity(now: Instant): AcknowledgmentLogEntity =
    AcknowledgmentLogEntity(
        reminderId = reminderId.value,
        reminderType = reminderType.wireName,
        scheduledAtEpochMillis = scheduledAt.toEpochMilli(),
        scheduledLocalDate = scheduledLocalDate.toString(),
        acknowledgedAtEpochMillis = acknowledgedAt.toEpochMilli(),
        source = source.wireName,
        createdAtEpochMillis = now.toEpochMilli(),
        updatedAtEpochMillis = now.toEpochMilli(),
    )

