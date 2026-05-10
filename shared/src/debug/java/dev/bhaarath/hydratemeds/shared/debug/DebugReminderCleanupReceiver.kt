package dev.bhaarath.hydratemeds.shared.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.work.WorkManager
import dev.bhaarath.hydratemeds.shared.model.ReminderId
import dev.bhaarath.hydratemeds.shared.runtime.ReminderNagWork
import dev.bhaarath.hydratemeds.shared.runtime.ReminderNotificationIds
import dev.bhaarath.hydratemeds.shared.runtime.ReminderRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DebugReminderCleanupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = ReminderRuntime.database(context)
                val ids = mutableSetOf<String>()
                db.query(
                    SimpleSQLiteQuery(
                        "SELECT reminder_id FROM active_reminders WHERE reminder_id LIKE 'debug-%'",
                    ),
                ).use { cursor ->
                    while (cursor.moveToNext()) ids += cursor.getString(0)
                }
                db.query(
                    SimpleSQLiteQuery(
                        "SELECT reminder_id FROM acknowledgment_logs WHERE reminder_id LIKE 'debug-%'",
                    ),
                ).use { cursor ->
                    while (cursor.moveToNext()) ids += cursor.getString(0)
                }

                val notifications = NotificationManagerCompat.from(context)
                ids.forEach { reminderId ->
                    ReminderNagWork.cancel(context, reminderId)
                    notifications.cancel(ReminderNotificationIds.notificationId(ReminderId(reminderId)))
                }
                notifications.cancelAll()
                WorkManager.getInstance(context).cancelAllWork()

                db.openHelper.writableDatabase.execSQL(
                    "DELETE FROM active_reminders WHERE reminder_id LIKE 'debug-%'",
                )
                db.openHelper.writableDatabase.execSQL(
                    "DELETE FROM acknowledgment_logs WHERE reminder_id LIKE 'debug-%'",
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
