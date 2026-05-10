package dev.bhaarath.hydratemeds.shared.runtime

import android.content.Context

object ReminderBootstrapper {
    fun start(context: Context) {
        ReminderNotifier(context).ensureChannel()
        ReminderAlarmScheduler(context).scheduleRestOfTodayAndTomorrowFirst()
    }
}
