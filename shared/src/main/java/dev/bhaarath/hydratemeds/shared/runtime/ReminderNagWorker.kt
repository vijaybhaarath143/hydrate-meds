package dev.bhaarath.hydratemeds.shared.runtime

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.bhaarath.hydratemeds.shared.schedule.HydrateMedsScheduleConfig
import java.time.Instant

class ReminderNagWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val payload = inputData.toReminderPayload()
        val db = ReminderRuntime.database(applicationContext)

        val acknowledged = db.acknowledgmentLogDao()
            .getByReminderId(payload.reminderId.value) != null
        val active = db.activeReminderDao()
            .getByReminderId(payload.reminderId.value) != null
        val event = payload.toReminderEvent()
        val paused = ReminderRuntime.repository(applicationContext).isPaused(event)
        val expired = Instant.now() >= event.scheduledAt.plus(
            HydrateMedsScheduleConfig.nagWindow(event.type),
        )

        if (acknowledged || !active) return Result.success()
        if (paused || expired) {
            db.activeReminderDao().clear(payload.reminderId.value)
            ReminderNotifier(applicationContext).dismiss(payload.reminderId)
            return Result.success()
        }

        db.activeReminderDao().upsert(
            db.activeReminderDao().getByReminderId(payload.reminderId.value)!!
                .copy(lastNotifiedAtEpochMillis = Instant.now().toEpochMilli()),
        )
        ReminderNotifier(applicationContext).show(payload)
        ReminderNagWork.enqueue(applicationContext, payload)
        return Result.success()
    }
}
