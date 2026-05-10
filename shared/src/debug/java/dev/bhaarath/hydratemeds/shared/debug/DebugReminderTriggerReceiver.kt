package dev.bhaarath.hydratemeds.shared.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.bhaarath.hydratemeds.shared.model.ReminderId
import dev.bhaarath.hydratemeds.shared.model.ReminderType
import dev.bhaarath.hydratemeds.shared.runtime.ReminderNagWork
import dev.bhaarath.hydratemeds.shared.runtime.ReminderNotifier
import dev.bhaarath.hydratemeds.shared.runtime.ReminderPayload
import dev.bhaarath.hydratemeds.shared.runtime.ReminderRuntime
import dev.bhaarath.hydratemeds.shared.runtime.toReminderEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

class DebugReminderTriggerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = Instant.now()
                val type = intent.getStringExtra("reminder_type")
                    ?.let(ReminderType::fromWireName)
                    ?: ReminderType.Water
                val title = intent.getStringExtra("title") ?: defaultTitle(type)
                val scheduledAt = intent.getLongExtra("scheduled_at_epoch_millis", 0L)
                    .takeIf { it > 0L }
                    ?.let(Instant::ofEpochMilli)
                    ?: now
                val reminderId = intent.getStringExtra("reminder_id")
                    ?: "debug-${type.wireName}-${scheduledAt.toEpochMilli()}"
                val payload = ReminderPayload(
                    reminderId = ReminderId(reminderId),
                    reminderType = type,
                    title = title,
                    scheduledAt = scheduledAt,
                    scheduledLocalDate = scheduledAt.atZone(ZoneId.systemDefault()).toLocalDate(),
                )
                ReminderRuntime.repository(context).markActive(
                    event = payload.toReminderEvent(),
                    firedAt = now,
                    lastNotifiedAt = now,
                )
                ReminderNotifier(context).show(payload)
                if (intent.getBooleanExtra("repeat_nag", false)) {
                    ReminderNagWork.enqueue(context, payload)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun defaultTitle(type: ReminderType): String =
        when (type) {
            ReminderType.Water -> "Drink water"
            ReminderType.MorningMedicine -> "Take morning medicine"
            ReminderType.EveningMedicine -> "Take evening medicine"
        }
}
