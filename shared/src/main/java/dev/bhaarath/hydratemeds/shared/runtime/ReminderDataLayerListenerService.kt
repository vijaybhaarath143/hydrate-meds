package dev.bhaarath.hydratemeds.shared.runtime

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dev.bhaarath.hydratemeds.shared.sync.DataLayerCodecs
import dev.bhaarath.hydratemeds.shared.sync.DataLayerPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderDataLayerListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        CoroutineScope(Dispatchers.IO).launch {
            when (messageEvent.path) {
                DataLayerPaths.acknowledgmentMessage -> {
                    val log = DataLayerCodecs.acknowledgmentFromBytes(messageEvent.data)
                    ReminderRuntime.repository(applicationContext).acknowledge(log)
                    ReminderNagWork.cancel(applicationContext, log.reminderId.value)
                    ReminderNotifier(applicationContext).dismiss(log.reminderId)
                }
                DataLayerPaths.pauseMessage -> {
                    val pause = DataLayerCodecs.dailyPauseFromBytes(messageEvent.data)
                    ReminderPauseCoordinator.setPaused(
                        context = applicationContext,
                        date = pause.date,
                        reminderType = pause.reminderType,
                        paused = pause.paused,
                    )
                }
            }
        }
    }
}
