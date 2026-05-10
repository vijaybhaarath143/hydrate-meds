package dev.bhaarath.hydratemeds.shared.runtime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderExtras.actionFireReminder) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val payload = intent.toReminderPayload()
                val db = ReminderRuntime.database(context)
                val event = payload.toReminderEvent()
                if (ReminderRuntime.repository(context).isPaused(event)) {
                    ReminderNagWork.cancel(context, payload.reminderId.value)
                    ReminderNotifier(context).dismiss(payload.reminderId)
                    ReminderRuntime.repository(context).clearActive(payload.reminderId)
                } else if (db.acknowledgmentLogDao().getByReminderId(payload.reminderId.value) == null) {
                    val repository = ReminderRuntime.repository(context)
                    repository.markActive(
                        event = event,
                        firedAt = Instant.now(),
                        lastNotifiedAt = Instant.now(),
                    )
                    ReminderNotifier(context).show(payload)
                    ReminderNagWork.enqueue(context, payload)
                }

                ReminderAlarmScheduler(context).scheduleRestOfTodayAndTomorrowFirst()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
