package dev.bhaarath.hydratemeds.shared.runtime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.bhaarath.hydratemeds.shared.database.toModel
import dev.bhaarath.hydratemeds.shared.model.AcknowledgmentSource
import dev.bhaarath.hydratemeds.shared.model.ReminderType
import dev.bhaarath.hydratemeds.shared.sync.WearableReminderDataLayerSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

class QuickLogWaterReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderExtras.actionQuickLogWater) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = ReminderRuntime.repository(context)
                val db = ReminderRuntime.database(context)
                val today = LocalDate.now()
                val loggedIds = db.acknowledgmentLogDao()
                    .getForLocalDate(today.toString())
                    .mapTo(mutableSetOf()) { it.toModel().reminderId }
                val now = Instant.now()
                val event = repository.scheduledEventsForDate(today)
                    .lastOrNull {
                        it.type == ReminderType.Water &&
                            it.scheduledAt <= now &&
                            it.id !in loggedIds
                    }
                    ?: return@launch
                val source = if (ReminderRuntime.isWatch(context)) {
                    AcknowledgmentSource.Watch
                } else {
                    AcknowledgmentSource.Phone
                }
                val log = repository.acknowledge(
                    event = event,
                    acknowledgedAt = Instant.now(),
                    source = source,
                )
                runCatching {
                    WearableReminderDataLayerSync(context.applicationContext).sendAcknowledgment(log)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
