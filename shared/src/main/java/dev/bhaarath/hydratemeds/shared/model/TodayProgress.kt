package dev.bhaarath.hydratemeds.shared.model

import java.time.Instant
import java.time.LocalDate

data class TodayProgress(
    val date: LocalDate,
    val waterAcknowledged: Int,
    val waterTarget: Int,
    val morningMedicineDone: Boolean,
    val eveningMedicineDone: Boolean,
    val nextReminderAt: Instant?,
    val updatedAt: Instant,
)

