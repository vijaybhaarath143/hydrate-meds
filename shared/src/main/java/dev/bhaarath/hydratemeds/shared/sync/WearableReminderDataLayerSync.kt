package dev.bhaarath.hydratemeds.shared.sync

import android.content.Context
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bhaarath.hydratemeds.shared.model.AcknowledgmentLog
import dev.bhaarath.hydratemeds.shared.model.ActiveReminder
import dev.bhaarath.hydratemeds.shared.model.DailyReminderPause
import dev.bhaarath.hydratemeds.shared.model.TodayProgress
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearableReminderDataLayerSync @Inject constructor(
    @ApplicationContext context: Context,
) : ReminderDataLayerSync {
    private val dataClient = Wearable.getDataClient(context)
    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    override suspend fun sendAcknowledgment(log: AcknowledgmentLog) {
        val payload = DataLayerCodecs.acknowledgmentToBytes(log)
        sendMessageToConnectedNodes(DataLayerPaths.acknowledgmentMessage, payload)
    }

    override suspend fun sendDailyPause(pause: DailyReminderPause) {
        val payload = DataLayerCodecs.dailyPauseToBytes(pause)
        sendMessageToConnectedNodes(DataLayerPaths.pauseMessage, payload)
    }

    private suspend fun sendMessageToConnectedNodes(path: String, payload: ByteArray) {
        nodeClient.connectedNodes.await().forEach { node ->
            messageClient.sendMessage(
                node.id,
                path,
                payload,
            ).await()
        }
    }

    override suspend fun publishActiveReminders(reminders: List<ActiveReminder>) {
        publishDataMap(
            path = DataLayerPaths.activeRemindersState,
            dataMap = DataLayerCodecs.activeRemindersToDataMap(
                reminders = reminders,
                updatedAt = Instant.now(),
            ),
        )
    }

    override suspend fun publishTodayProgress(progress: TodayProgress) {
        publishDataMap(
            path = DataLayerPaths.todayProgressState,
            dataMap = DataLayerCodecs.todayProgressToDataMap(progress),
        )
    }

    private suspend fun publishDataMap(path: String, dataMap: DataMap) {
        val request = PutDataMapRequest.create(path).apply {
            this.dataMap.putAll(dataMap)
        }.asPutDataRequest().setUrgent()

        dataClient.putDataItem(request).await()
    }
}
