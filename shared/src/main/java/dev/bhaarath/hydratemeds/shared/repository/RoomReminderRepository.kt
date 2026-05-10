package dev.bhaarath.hydratemeds.shared.repository

import dev.bhaarath.hydratemeds.shared.database.AcknowledgmentLogDao
import dev.bhaarath.hydratemeds.shared.database.ActiveReminderDao
import dev.bhaarath.hydratemeds.shared.database.DailyReminderPauseDao
import dev.bhaarath.hydratemeds.shared.database.toEntity
import dev.bhaarath.hydratemeds.shared.database.toModel
import dev.bhaarath.hydratemeds.shared.model.AcknowledgmentLog
import dev.bhaarath.hydratemeds.shared.model.AcknowledgmentSource
import dev.bhaarath.hydratemeds.shared.model.ActiveReminder
import dev.bhaarath.hydratemeds.shared.model.DailyReminderPause
import dev.bhaarath.hydratemeds.shared.model.ReminderEvent
import dev.bhaarath.hydratemeds.shared.model.ReminderId
import dev.bhaarath.hydratemeds.shared.model.ReminderType
import dev.bhaarath.hydratemeds.shared.model.TodayProgress
import dev.bhaarath.hydratemeds.shared.schedule.HydrateMedsScheduleConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomReminderRepository @Inject constructor(
    private val acknowledgmentLogDao: AcknowledgmentLogDao,
    private val activeReminderDao: ActiveReminderDao,
    private val dailyReminderPauseDao: DailyReminderPauseDao,
) : ReminderRepository {
    override fun scheduledEventsForDate(
        date: LocalDate,
        zoneId: ZoneId,
    ): List<ReminderEvent> = HydrateMedsScheduleConfig.eventsForDate(date, zoneId)

    override fun observeAcknowledgmentsForDate(date: LocalDate): Flow<List<AcknowledgmentLog>> =
        acknowledgmentLogDao.observeForLocalDate(date.toString())
            .map { entities -> entities.map { it.toModel() } }

    override fun observeActiveReminders(): Flow<List<ActiveReminder>> =
        activeReminderDao.observeActiveReminders()
            .map { entities -> entities.map { it.toModel() } }

    override fun observePausedTypesForDate(date: LocalDate): Flow<Set<ReminderType>> =
        dailyReminderPauseDao.observeForLocalDate(date.toString())
            .map { entities -> entities.mapTo(mutableSetOf()) { it.toModel().reminderType } }

    override fun observeTodayProgress(
        date: LocalDate,
        zoneId: ZoneId,
    ): Flow<TodayProgress> =
        observeAcknowledgmentsForDate(date).map { logs ->
            val acknowledgedIds = logs.mapTo(mutableSetOf()) { it.reminderId }
            val events = HydrateMedsScheduleConfig.eventsForDate(date, zoneId)
            val now = Instant.now()
            TodayProgress(
                date = date,
                waterAcknowledged = logs.count { it.reminderType == ReminderType.Water },
                waterTarget = HydrateMedsScheduleConfig.dailyWaterTarget,
                morningMedicineDone = logs.any { it.reminderType == ReminderType.MorningMedicine },
                eveningMedicineDone = logs.any { it.reminderType == ReminderType.EveningMedicine },
                nextReminderAt = events.firstOrNull {
                    it.id !in acknowledgedIds && it.scheduledAt > now
                }?.scheduledAt,
                updatedAt = now,
            )
        }

    override suspend fun acknowledge(
        event: ReminderEvent,
        acknowledgedAt: Instant,
        source: AcknowledgmentSource,
    ): AcknowledgmentLog {
        val log = AcknowledgmentLog(
            reminderId = event.id,
            reminderType = event.type,
            scheduledAt = event.scheduledAt,
            scheduledLocalDate = event.localDate,
            acknowledgedAt = acknowledgedAt,
            source = source,
        )
        acknowledge(log)
        return log
    }

    override suspend fun acknowledge(log: AcknowledgmentLog) {
        val now = Instant.now()
        acknowledgmentLogDao.upsertEarliest(log.toEntity(now))
        activeReminderDao.clear(log.reminderId.value)
    }

    override suspend fun markActive(
        event: ReminderEvent,
        firedAt: Instant,
        lastNotifiedAt: Instant?,
    ) {
        activeReminderDao.upsert(
            ActiveReminder(
                reminderId = event.id,
                reminderType = event.type,
                title = event.title,
                scheduledAt = event.scheduledAt,
                scheduledLocalDate = event.localDate,
                firedAt = firedAt,
                lastNotifiedAt = lastNotifiedAt,
            ).toEntity(),
        )
    }

    override suspend fun clearActive(reminderId: ReminderId) {
        activeReminderDao.clear(reminderId.value)
    }

    override suspend fun setPausedForDate(
        date: LocalDate,
        reminderType: ReminderType,
        paused: Boolean,
    ) {
        if (paused) {
            dailyReminderPauseDao.upsert(
                DailyReminderPause(
                    date = date,
                    reminderType = reminderType,
                    paused = true,
                ).toEntity(nowEpochMillis = Instant.now().toEpochMilli()),
            )
        } else {
            dailyReminderPauseDao.delete(date.toString(), reminderType.wireName)
        }
    }

    override suspend fun isPaused(event: ReminderEvent): Boolean =
        dailyReminderPauseDao.isPaused(event.localDate.toString(), event.type.wireName)
}
