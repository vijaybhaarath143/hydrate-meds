package dev.bhaarath.hydratemeds.shared.model

import java.time.Instant
import java.time.LocalDate

data class ActiveReminder(
    val reminderId: ReminderId,
    val reminderType: ReminderType,
    val title: String,
    val scheduledAt: Instant,
    val scheduledLocalDate: LocalDate,
    val firedAt: Instant,
    val lastNotifiedAt: Instant?,
)

