package dev.bhaarath.hydratemeds.shared.runtime

import dev.bhaarath.hydratemeds.shared.model.ReminderEvent
import dev.bhaarath.hydratemeds.shared.schedule.HydrateMedsScheduleConfig

fun ReminderPayload.toReminderEvent(): ReminderEvent =
    ReminderEvent(
        id = reminderId,
        type = reminderType,
        title = title,
        scheduledAt = scheduledAt,
        localDate = scheduledLocalDate,
        localTime = scheduledAt.atZone(java.time.ZoneId.systemDefault()).toLocalTime(),
        nagInterval = HydrateMedsScheduleConfig.nagInterval,
        onTimeWindow = HydrateMedsScheduleConfig.onTimeWindow,
    )

