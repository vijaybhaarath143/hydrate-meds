package dev.bhaarath.hydratemeds.shared.runtime

import dev.bhaarath.hydratemeds.shared.model.ReminderId
import kotlin.math.absoluteValue

object ReminderNotificationIds {
    const val channelId = "hydrate_meds_reminders"

    fun notificationId(reminderId: ReminderId): Int =
        reminderId.value.hashCode().absoluteValue.coerceAtLeast(1)
}

