package dev.bhaarath.hydratemeds.mobile

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.bhaarath.hydratemeds.shared.runtime.ReminderBootstrapper

@HiltAndroidApp
class HydrateMedsMobileApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderBootstrapper.start(this)
    }
}

