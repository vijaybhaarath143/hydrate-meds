package dev.bhaarath.hydratemeds.shared.sync

import dev.bhaarath.hydratemeds.shared.model.AcknowledgmentLog
import dev.bhaarath.hydratemeds.shared.model.ActiveReminder
import dev.bhaarath.hydratemeds.shared.model.DailyReminderPause
import dev.bhaarath.hydratemeds.shared.model.TodayProgress

interface ReminderDataLayerSync {
    suspend fun sendAcknowledgment(log: AcknowledgmentLog)

    suspend fun sendDailyPause(pause: DailyReminderPause)

    suspend fun publishActiveReminders(reminders: List<ActiveReminder>)

    suspend fun publishTodayProgress(progress: TodayProgress)
}
