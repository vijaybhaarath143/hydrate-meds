package dev.bhaarath.hydratemeds.wear

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.bhaarath.hydratemeds.shared.runtime.ReminderBootstrapper

@HiltAndroidApp
class HydrateMedsWearApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderBootstrapper.start(this)
    }
}

