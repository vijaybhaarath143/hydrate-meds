package dev.bhaarath.hydratemeds.shared.runtime

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dev.bhaarath.hydratemeds.shared.model.ReminderEvent
import dev.bhaarath.hydratemeds.shared.schedule.HydrateMedsScheduleConfig
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderAlarmScheduler(
    private val context: Context,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun scheduleRestOfTodayAndTomorrowFirst(
        now: ZonedDateTime = ZonedDateTime.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ) {
        val today = HydrateMedsScheduleConfig.eventsForDate(now.toLocalDate(), zoneId)
            .filter { it.scheduledAt > now.toInstant() }
        val tomorrowFirst = HydrateMedsScheduleConfig.eventsForDate(
            now.toLocalDate().plusDays(1),
            zoneId,
        ).take(1)

        (today + tomorrowFirst).forEach(::schedule)
    }

    fun scheduleNextDay(date: LocalDate = LocalDate.now().plusDays(1)) {
        HydrateMedsScheduleConfig.eventsForDate(date).forEach(::schedule)
    }

    fun schedule(event: ReminderEvent) {
        if (Build.VERSION.SDK_INT >= 31 && !alarmManager.canScheduleExactAlarms()) return

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            event.scheduledAt.toEpochMilli(),
            pendingIntent(event),
        )
    }

    fun cancel(event: ReminderEvent) {
        alarmManager.cancel(pendingIntent(event))
    }

    private fun pendingIntent(event: ReminderEvent): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
            .setAction(ReminderExtras.actionFireReminder)
            .putReminder(event)

        return PendingIntent.getBroadcast(
            context,
            event.id.value.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

fun ReminderPayload.toAcknowledgmentLog(
    acknowledgedAt: Instant,
    source: dev.bhaarath.hydratemeds.shared.model.AcknowledgmentSource,
): dev.bhaarath.hydratemeds.shared.model.AcknowledgmentLog =
    dev.bhaarath.hydratemeds.shared.model.AcknowledgmentLog(
        reminderId = reminderId,
        reminderType = reminderType,
        scheduledAt = scheduledAt,
        scheduledLocalDate = scheduledLocalDate,
        acknowledgedAt = acknowledgedAt,
        source = source,
    )

