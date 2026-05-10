package dev.bhaarath.hydratemeds.shared.runtime

import android.content.Context
import dev.bhaarath.hydratemeds.shared.model.ReminderId
import dev.bhaarath.hydratemeds.shared.model.ReminderType
import java.time.LocalDate

object ReminderPauseCoordinator {
    suspend fun setPaused(
        context: Context,
        date: LocalDate,
        reminderType: ReminderType,
        paused: Boolean,
    ) {
        ReminderRuntime.repository(context).setPausedForDate(
            date = date,
            reminderType = reminderType,
            paused = paused,
        )
        if (paused) {
            clearActiveFor(context, date, reminderType)
        }
    }

    suspend fun clearActiveFor(
        context: Context,
        date: LocalDate,
        reminderType: ReminderType,
    ) {
        val db = ReminderRuntime.database(context)
        val active = db.activeReminderDao().getForLocalDateAndType(
            localDate = date.toString(),
            reminderType = reminderType.wireName,
        )
        val notifier = ReminderNotifier(context)
        active.forEach { entity ->
            ReminderNagWork.cancel(context, entity.reminderId)
            notifier.dismiss(ReminderId(entity.reminderId))
            db.activeReminderDao().clear(entity.reminderId)
        }
    }
}
