package dev.bhaarath.hydratemeds.shared.model

import java.time.LocalDate

data class DailyReminderPause(
    val date: LocalDate,
    val reminderType: ReminderType,
    val paused: Boolean,
)
