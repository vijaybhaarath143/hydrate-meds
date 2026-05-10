package dev.bhaarath.hydratemeds.shared.runtime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.bhaarath.hydratemeds.shared.model.AcknowledgmentSource
import dev.bhaarath.hydratemeds.shared.sync.WearableReminderDataLayerSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

class AcknowledgeReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderExtras.actionAcknowledgeReminder) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val payload = intent.toReminderPayload()
                val source = if (ReminderRuntime.isWatch(context)) {
                    AcknowledgmentSource.Watch
                } else {
                    AcknowledgmentSource.Phone
                }
                val log = payload.toAcknowledgmentLog(
                    acknowledgedAt = Instant.now(),
                    source = source,
                )

                ReminderRuntime.repository(context).acknowledge(log)
                ReminderNagWork.cancel(context, payload.reminderId.value)
                ReminderNotifier(context).dismiss(payload.reminderId)
                WearableReminderDataLayerSync(context.applicationContext).sendAcknowledgment(log)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

