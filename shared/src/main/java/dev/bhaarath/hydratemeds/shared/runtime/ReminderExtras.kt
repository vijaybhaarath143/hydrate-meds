package dev.bhaarath.hydratemeds.shared.runtime

import android.content.Intent
import androidx.work.Data
import dev.bhaarath.hydratemeds.shared.model.ReminderEvent
import dev.bhaarath.hydratemeds.shared.model.ReminderId
import dev.bhaarath.hydratemeds.shared.model.ReminderType
import java.time.Instant
import java.time.LocalDate

object ReminderExtras {
    const val actionFireReminder = "dev.bhaarath.hydratemeds.action.FIRE_REMINDER"
    const val actionAcknowledgeReminder = "dev.bhaarath.hydratemeds.action.ACKNOWLEDGE_REMINDER"
    const val actionQuickLogWater = "dev.bhaarath.hydratemeds.action.QUICK_LOG_WATER"

    const val keyReminderId = "reminder_id"
    const val keyReminderType = "reminder_type"
    const val keyTitle = "title"
    const val keyScheduledAt = "scheduled_at_epoch_millis"
    const val keyScheduledLocalDate = "scheduled_local_date"
}

fun Intent.putReminder(event: ReminderEvent): Intent = apply {
    putExtra(ReminderExtras.keyReminderId, event.id.value)
    putExtra(ReminderExtras.keyReminderType, event.type.wireName)
    putExtra(ReminderExtras.keyTitle, event.title)
    putExtra(ReminderExtras.keyScheduledAt, event.scheduledAt.toEpochMilli())
    putExtra(ReminderExtras.keyScheduledLocalDate, event.localDate.toString())
}

fun Intent.toReminderPayload(): ReminderPayload =
    ReminderPayload(
        reminderId = ReminderId(requireNotNull(getStringExtra(ReminderExtras.keyReminderId))),
        reminderType = ReminderType.fromWireName(
            requireNotNull(getStringExtra(ReminderExtras.keyReminderType)),
        ),
        title = requireNotNull(getStringExtra(ReminderExtras.keyTitle)),
        scheduledAt = Instant.ofEpochMilli(getLongExtra(ReminderExtras.keyScheduledAt, 0L)),
        scheduledLocalDate = LocalDate.parse(
            requireNotNull(getStringExtra(ReminderExtras.keyScheduledLocalDate)),
        ),
    )

fun ReminderPayload.toWorkData(): Data =
    Data.Builder()
        .putString(ReminderExtras.keyReminderId, reminderId.value)
        .putString(ReminderExtras.keyReminderType, reminderType.wireName)
        .putString(ReminderExtras.keyTitle, title)
        .putLong(ReminderExtras.keyScheduledAt, scheduledAt.toEpochMilli())
        .putString(ReminderExtras.keyScheduledLocalDate, scheduledLocalDate.toString())
        .build()

fun Data.toReminderPayload(): ReminderPayload =
    ReminderPayload(
        reminderId = ReminderId(requireNotNull(getString(ReminderExtras.keyReminderId))),
        reminderType = ReminderType.fromWireName(
            requireNotNull(getString(ReminderExtras.keyReminderType)),
        ),
        title = requireNotNull(getString(ReminderExtras.keyTitle)),
        scheduledAt = Instant.ofEpochMilli(getLong(ReminderExtras.keyScheduledAt, 0L)),
        scheduledLocalDate = LocalDate.parse(
            requireNotNull(getString(ReminderExtras.keyScheduledLocalDate)),
        ),
    )

data class ReminderPayload(
    val reminderId: ReminderId,
    val reminderType: ReminderType,
    val title: String,
    val scheduledAt: Instant,
    val scheduledLocalDate: LocalDate,
)
