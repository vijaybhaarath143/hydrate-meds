package dev.bhaarath.hydratemeds.shared.runtime

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dev.bhaarath.hydratemeds.shared.schedule.HydrateMedsScheduleConfig
import java.util.concurrent.TimeUnit

object ReminderNagWork {
    fun name(reminderId: String): String = "nag:$reminderId"

    fun enqueue(context: android.content.Context, payload: ReminderPayload) {
        val request = OneTimeWorkRequestBuilder<ReminderNagWorker>()
            .setInitialDelay(HydrateMedsScheduleConfig.nagIntervalMinutes, TimeUnit.MINUTES)
            .setInputData(payload.toWorkData())
            .addTag(name(payload.reminderId.value))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            name(payload.reminderId.value),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel(context: android.content.Context, reminderId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(name(reminderId))
    }
}
