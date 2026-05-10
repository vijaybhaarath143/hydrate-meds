package dev.bhaarath.hydratemeds.shared.sync

import com.google.android.gms.wearable.DataMap
import dev.bhaarath.hydratemeds.shared.model.AcknowledgmentLog
import dev.bhaarath.hydratemeds.shared.model.AcknowledgmentSource
import dev.bhaarath.hydratemeds.shared.model.ActiveReminder
import dev.bhaarath.hydratemeds.shared.model.DailyReminderPause
import dev.bhaarath.hydratemeds.shared.model.ReminderId
import dev.bhaarath.hydratemeds.shared.model.ReminderType
import dev.bhaarath.hydratemeds.shared.model.TodayProgress
import java.time.Instant
import java.time.LocalDate

object DataLayerCodecs {
    private const val keyReminderId = "reminder_id"
    private const val keyReminderType = "reminder_type"
    private const val keyTitle = "title"
    private const val keyScheduledAt = "scheduled_at_epoch_millis"
    private const val keyScheduledLocalDate = "scheduled_local_date"
    private const val keyAcknowledgedAt = "acknowledged_at_epoch_millis"
    private const val keySource = "source"
    private const val keyFiredAt = "fired_at_epoch_millis"
    private const val keyLastNotifiedAt = "last_notified_at_epoch_millis"
    private const val keyDate = "date"
    private const val keyWaterAcknowledged = "water_acknowledged"
    private const val keyWaterTarget = "water_target"
    private const val keyMorningMedicineDone = "morning_medicine_done"
    private const val keyEveningMedicineDone = "evening_medicine_done"
    private const val keyNextReminderAt = "next_reminder_at_epoch_millis"
    private const val keyUpdatedAt = "updated_at_epoch_millis"
    private const val keyItems = "items"
    private const val keyPaused = "paused"
    private const val nullInstant = -1L

    fun acknowledgmentToBytes(log: AcknowledgmentLog): ByteArray =
        acknowledgmentToDataMap(log).toByteArray()

    fun acknowledgmentFromBytes(bytes: ByteArray): AcknowledgmentLog =
        acknowledgmentFromDataMap(DataMap.fromByteArray(bytes))

    fun acknowledgmentToDataMap(log: AcknowledgmentLog): DataMap =
        DataMap().apply {
            putString(keyReminderId, log.reminderId.value)
            putString(keyReminderType, log.reminderType.wireName)
            putLong(keyScheduledAt, log.scheduledAt.toEpochMilli())
            putString(keyScheduledLocalDate, log.scheduledLocalDate.toString())
            putLong(keyAcknowledgedAt, log.acknowledgedAt.toEpochMilli())
            putString(keySource, log.source.wireName)
        }

    fun acknowledgmentFromDataMap(dataMap: DataMap): AcknowledgmentLog =
        AcknowledgmentLog(
            reminderId = ReminderId(dataMap.getString(keyReminderId).orEmpty()),
            reminderType = ReminderType.fromWireName(dataMap.getString(keyReminderType).orEmpty()),
            scheduledAt = Instant.ofEpochMilli(dataMap.getLong(keyScheduledAt)),
            scheduledLocalDate = LocalDate.parse(dataMap.getString(keyScheduledLocalDate)),
            acknowledgedAt = Instant.ofEpochMilli(dataMap.getLong(keyAcknowledgedAt)),
            source = AcknowledgmentSource.fromWireName(dataMap.getString(keySource).orEmpty()),
        )

    fun dailyPauseToBytes(pause: DailyReminderPause): ByteArray =
        DataMap().apply {
            putString(keyDate, pause.date.toString())
            putString(keyReminderType, pause.reminderType.wireName)
            putBoolean(keyPaused, pause.paused)
        }.toByteArray()

    fun dailyPauseFromBytes(bytes: ByteArray): DailyReminderPause {
        val dataMap = DataMap.fromByteArray(bytes)
        return DailyReminderPause(
            date = LocalDate.parse(dataMap.getString(keyDate)),
            reminderType = ReminderType.fromWireName(dataMap.getString(keyReminderType).orEmpty()),
            paused = dataMap.getBoolean(keyPaused),
        )
    }

    fun activeRemindersToDataMap(reminders: List<ActiveReminder>, updatedAt: Instant): DataMap =
        DataMap().apply {
            putLong(keyUpdatedAt, updatedAt.toEpochMilli())
            putDataMapArrayList(
                keyItems,
                ArrayList(reminders.map { it.toDataMap() }),
            )
        }

    fun activeRemindersFromDataMap(dataMap: DataMap): List<ActiveReminder> =
        dataMap.getDataMapArrayList(keyItems)
            ?.map { it.toActiveReminder() }
            .orEmpty()

    fun todayProgressToDataMap(progress: TodayProgress): DataMap =
        DataMap().apply {
            putString(keyDate, progress.date.toString())
            putInt(keyWaterAcknowledged, progress.waterAcknowledged)
            putInt(keyWaterTarget, progress.waterTarget)
            putBoolean(keyMorningMedicineDone, progress.morningMedicineDone)
            putBoolean(keyEveningMedicineDone, progress.eveningMedicineDone)
            putLong(keyNextReminderAt, progress.nextReminderAt?.toEpochMilli() ?: nullInstant)
            putLong(keyUpdatedAt, progress.updatedAt.toEpochMilli())
        }

    fun todayProgressFromDataMap(dataMap: DataMap): TodayProgress {
        val nextReminderAt = dataMap.getLong(keyNextReminderAt, nullInstant)
            .takeIf { it != nullInstant }
            ?.let(Instant::ofEpochMilli)

        return TodayProgress(
            date = LocalDate.parse(dataMap.getString(keyDate)),
            waterAcknowledged = dataMap.getInt(keyWaterAcknowledged),
            waterTarget = dataMap.getInt(keyWaterTarget),
            morningMedicineDone = dataMap.getBoolean(keyMorningMedicineDone),
            eveningMedicineDone = dataMap.getBoolean(keyEveningMedicineDone),
            nextReminderAt = nextReminderAt,
            updatedAt = Instant.ofEpochMilli(dataMap.getLong(keyUpdatedAt)),
        )
    }

    private fun ActiveReminder.toDataMap(): DataMap =
        DataMap().apply {
            putString(keyReminderId, reminderId.value)
            putString(keyReminderType, reminderType.wireName)
            putString(keyTitle, title)
            putLong(keyScheduledAt, scheduledAt.toEpochMilli())
            putString(keyScheduledLocalDate, scheduledLocalDate.toString())
            putLong(keyFiredAt, firedAt.toEpochMilli())
            putLong(keyLastNotifiedAt, lastNotifiedAt?.toEpochMilli() ?: nullInstant)
        }

    private fun DataMap.toActiveReminder(): ActiveReminder =
        ActiveReminder(
            reminderId = ReminderId(getString(keyReminderId).orEmpty()),
            reminderType = ReminderType.fromWireName(getString(keyReminderType).orEmpty()),
            title = getString(keyTitle).orEmpty(),
            scheduledAt = Instant.ofEpochMilli(getLong(keyScheduledAt)),
            scheduledLocalDate = LocalDate.parse(getString(keyScheduledLocalDate)),
            firedAt = Instant.ofEpochMilli(getLong(keyFiredAt)),
            lastNotifiedAt = getLong(keyLastNotifiedAt, nullInstant)
                .takeIf { it != nullInstant }
                ?.let(Instant::ofEpochMilli),
        )
}
