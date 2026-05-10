package dev.bhaarath.hydratemeds.shared.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.bhaarath.hydratemeds.shared.model.ReminderType
import dev.bhaarath.hydratemeds.shared.runtime.ReminderRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class DebugWaterLogRepairReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val zoneId = ZoneId.systemDefault()
                val now = ZonedDateTime.now(zoneId)
                val date = intent.getStringExtra("local_date")
                    ?.let(LocalDate::parse)
                    ?: now.toLocalDate()
                val fromHour = intent.getIntExtra("from_hour", now.hour)
                    .coerceIn(0, 23)
                val fromEpochMillis = date.atTime(fromHour, 0)
                    .atZone(zoneId)
                    .toInstant()
                    .toEpochMilli()

                ReminderRuntime.database(context).openHelper.writableDatabase.execSQL(
                    """
                    DELETE FROM acknowledgment_logs
                    WHERE scheduled_local_date = ?
                      AND reminder_type = ?
                      AND acknowledged_at_epoch_millis < scheduled_at_epoch_millis
                      AND scheduled_at_epoch_millis >= ?
                    """.trimIndent(),
                    arrayOf<Any>(
                        date.toString(),
                        ReminderType.Water.wireName,
                        fromEpochMillis,
                    ),
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
