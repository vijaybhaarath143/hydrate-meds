package dev.bhaarath.hydratemeds.shared.schedule

import dev.bhaarath.hydratemeds.shared.model.ReminderEvent
import dev.bhaarath.hydratemeds.shared.model.ReminderId
import dev.bhaarath.hydratemeds.shared.model.ReminderType
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object HydrateMedsScheduleConfig {
    const val waterStartHour = 7
    const val waterEndHourInclusive = 21
    const val nagIntervalMinutes = 5L
    const val onTimeWindowMinutes = 5L
    const val waterNagWindowMinutes = 55L
    const val medicineNagWindowMinutes = 30L
    const val spokenAlertsEnabled = true

    val waterTimes: List<LocalTime> =
        (waterStartHour..waterEndHourInclusive).map { hour -> LocalTime.of(hour, 0) }

    val medicineTimes: List<MedicineReminderConfig> = listOf(
        MedicineReminderConfig(
            type = ReminderType.MorningMedicine,
            time = LocalTime.of(10, 0),
            title = "Take morning medicine",
        ),
        MedicineReminderConfig(
            type = ReminderType.EveningMedicine,
            time = LocalTime.of(20, 30),
            title = "Take evening medicine",
        ),
    )

    val dailyWaterTarget: Int = waterTimes.size
    val dailyReminderCount: Int = waterTimes.size + medicineTimes.size
    val nagInterval: Duration = Duration.ofMinutes(nagIntervalMinutes)
    val onTimeWindow: Duration = Duration.ofMinutes(onTimeWindowMinutes)
    val waterNagWindow: Duration = Duration.ofMinutes(waterNagWindowMinutes)
    val medicineNagWindow: Duration = Duration.ofMinutes(medicineNagWindowMinutes)

    fun spokenPrompt(type: ReminderType): String =
        when (type) {
            ReminderType.Water -> "Drink water."
            ReminderType.MorningMedicine -> "Take morning medicine."
            ReminderType.EveningMedicine -> "Take evening medicine."
        }

    fun nagWindow(type: ReminderType): Duration =
        when (type) {
            ReminderType.Water -> waterNagWindow
            ReminderType.MorningMedicine,
            ReminderType.EveningMedicine -> medicineNagWindow
        }

    fun eventsForDate(
        date: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<ReminderEvent> {
        val waterEvents = waterTimes.map { time ->
            eventFor(
                type = ReminderType.Water,
                title = "Drink water",
                date = date,
                time = time,
                zoneId = zoneId,
            )
        }
        val medicineEvents = medicineTimes.map { config ->
            eventFor(
                type = config.type,
                title = config.title,
                date = date,
                time = config.time,
                zoneId = zoneId,
            )
        }

        return (waterEvents + medicineEvents).sortedBy { it.scheduledAt }
    }

    fun nextEventAfter(
        now: ZonedDateTime,
        zoneId: ZoneId = now.zone,
    ): ReminderEvent {
        val today = eventsForDate(now.toLocalDate(), zoneId)
            .firstOrNull { it.scheduledAt > now.toInstant() }

        return today ?: eventsForDate(now.toLocalDate().plusDays(1), zoneId).first()
    }

    private fun eventFor(
        type: ReminderType,
        title: String,
        date: LocalDate,
        time: LocalTime,
        zoneId: ZoneId,
    ): ReminderEvent {
        val scheduledAt = date.atTime(time).atZone(zoneId).toInstant()
        return ReminderEvent(
            id = ReminderId.forScheduled(type, scheduledAt),
            type = type,
            title = title,
            scheduledAt = scheduledAt,
            localDate = date,
            localTime = time,
            nagInterval = nagInterval,
            onTimeWindow = onTimeWindow,
        )
    }
}

data class MedicineReminderConfig(
    val type: ReminderType,
    val time: LocalTime,
    val title: String,
)
