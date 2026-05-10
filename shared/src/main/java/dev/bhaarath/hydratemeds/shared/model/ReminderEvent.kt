package dev.bhaarath.hydratemeds.shared.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class ReminderEvent(
    val id: ReminderId,
    val type: ReminderType,
    val title: String,
    val scheduledAt: Instant,
    val localDate: LocalDate,
    val localTime: LocalTime,
    val nagInterval: Duration,
    val onTimeWindow: Duration,
)

