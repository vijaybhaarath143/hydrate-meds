package dev.bhaarath.hydratemeds.shared.repository

import dev.bhaarath.hydratemeds.shared.model.AcknowledgmentLog
import dev.bhaarath.hydratemeds.shared.model.AcknowledgmentSource
import dev.bhaarath.hydratemeds.shared.model.ActiveReminder
import dev.bhaarath.hydratemeds.shared.model.ReminderEvent
import dev.bhaarath.hydratemeds.shared.model.ReminderId
import dev.bhaarath.hydratemeds.shared.model.ReminderType
import dev.bhaarath.hydratemeds.shared.model.TodayProgress
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

interface ReminderRepository {
    fun scheduledEventsForDate(
        date: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<ReminderEvent>

    fun observeAcknowledgmentsForDate(date: LocalDate): Flow<List<AcknowledgmentLog>>

    fun observeActiveReminders(): Flow<List<ActiveReminder>>

    fun observePausedTypesForDate(date: LocalDate): Flow<Set<ReminderType>>

    fun observeTodayProgress(
        date: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Flow<TodayProgress>

    suspend fun acknowledge(
        event: ReminderEvent,
        acknowledgedAt: Instant,
        source: AcknowledgmentSource,
    ): AcknowledgmentLog

    suspend fun acknowledge(log: AcknowledgmentLog)

    suspend fun markActive(
        event: ReminderEvent,
        firedAt: Instant,
        lastNotifiedAt: Instant?,
    )

    suspend fun clearActive(reminderId: ReminderId)

    suspend fun setPausedForDate(
        date: LocalDate,
        reminderType: ReminderType,
        paused: Boolean,
    )

    suspend fun isPaused(event: ReminderEvent): Boolean
}
