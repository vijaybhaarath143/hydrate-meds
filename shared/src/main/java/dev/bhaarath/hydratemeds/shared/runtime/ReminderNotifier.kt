package dev.bhaarath.hydratemeds.shared.runtime

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.bhaarath.hydratemeds.shared.model.ReminderType

class ReminderNotifier(
    private val context: Context,
) {
    fun show(payload: ReminderPayload) {
        ensureChannel()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val doneIntent = Intent(context, AcknowledgeReminderReceiver::class.java)
            .setAction(ReminderExtras.actionAcknowledgeReminder)
            .putPayload(payload)

        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            payload.reminderId.value.hashCode(),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val icon = android.R.drawable.ic_dialog_info
        val notification = NotificationCompat.Builder(context, ReminderNotificationIds.channelId)
            .setSmallIcon(icon)
            .setContentTitle(payload.title)
            .setContentText(contentText(payload.reminderType))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setVibrate(longArrayOf(0, 80, 40, 120))
            .setContentIntent(donePendingIntent)
            .addAction(icon, "Done", donePendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(
            ReminderNotificationIds.notificationId(payload.reminderId),
            notification,
        )
        ReminderVoiceAnnouncer.speak(context, payload)
    }

    fun dismiss(reminderId: dev.bhaarath.hydratemeds.shared.model.ReminderId) {
        NotificationManagerCompat.from(context).cancel(
            ReminderNotificationIds.notificationId(reminderId),
        )
    }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < 26) return

        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            ReminderNotificationIds.channelId,
            "Hydrate & Meds reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Water and medicine reminders"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    private fun contentText(type: ReminderType): String =
        when (type) {
            ReminderType.Water -> "Tap Done once you've had water."
            ReminderType.MorningMedicine,
            ReminderType.EveningMedicine -> "Tap Done after taking it."
        }

    private fun Intent.putPayload(payload: ReminderPayload): Intent = apply {
        putExtra("reminder_id", payload.reminderId.value)
        putExtra("reminder_type", payload.reminderType.wireName)
        putExtra("title", payload.title)
        putExtra("scheduled_at_epoch_millis", payload.scheduledAt.toEpochMilli())
        putExtra("scheduled_local_date", payload.scheduledLocalDate.toString())
    }
}
