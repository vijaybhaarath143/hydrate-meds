package dev.bhaarath.hydratemeds.wear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bhaarath.hydratemeds.shared.model.AcknowledgmentLog
import dev.bhaarath.hydratemeds.shared.model.AcknowledgmentSource
import dev.bhaarath.hydratemeds.shared.model.ActiveReminder
import dev.bhaarath.hydratemeds.shared.model.ReminderEvent
import dev.bhaarath.hydratemeds.shared.model.ReminderType
import dev.bhaarath.hydratemeds.shared.repository.ReminderRepository
import dev.bhaarath.hydratemeds.shared.schedule.HydrateMedsScheduleConfig
import dev.bhaarath.hydratemeds.shared.sync.ReminderDataLayerSync
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class WearHomeViewModel @Inject constructor(
    private val repository: ReminderRepository,
    private val dataLayerSync: ReminderDataLayerSync,
) : ViewModel() {
    private val today = LocalDate.now()

    val uiState = combine(
        repository.observeAcknowledgmentsForDate(today),
        repository.observeActiveReminders(),
    ) { logs, active ->
        val zoneId = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zoneId)
        val events = repository.scheduledEventsForDate(today, zoneId)
        val activeIds = active.mapTo(mutableSetOf()) { it.reminderId }
        val activeEvent = events.firstOrNull { it.id in activeIds }
        WearUiState(
            waterDone = logs.count { it.reminderType == ReminderType.Water },
            waterTarget = HydrateMedsScheduleConfig.dailyWaterTarget,
            morningDone = logs.any { it.reminderType == ReminderType.MorningMedicine },
            eveningDone = logs.any { it.reminderType == ReminderType.EveningMedicine },
            activeEvent = activeEvent,
            nextEvent = HydrateMedsScheduleConfig.nextEventAfter(now, zoneId),
            loggedIds = logs.mapTo(mutableSetOf()) { it.reminderId.value },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WearUiState(),
    )

    fun logWater() {
        viewModelScope.launch {
            val state = uiState.value
            val now = Instant.now()
            val event = repository.scheduledEventsForDate(today)
                .lastOrNull {
                    it.type == ReminderType.Water &&
                        it.scheduledAt <= now &&
                        it.id.value !in state.loggedIds
                }
                ?: return@launch
            acknowledge(event)
        }
    }

    fun acknowledgeActive() {
        viewModelScope.launch {
            val event = uiState.value.activeEvent ?: return@launch
            acknowledge(event)
        }
    }

    private suspend fun acknowledge(event: ReminderEvent): AcknowledgmentLog {
        val log = repository.acknowledge(
            event = event,
            acknowledgedAt = Instant.now(),
            source = AcknowledgmentSource.Watch,
        )
        runCatching { dataLayerSync.sendAcknowledgment(log) }
        return log
    }
}

data class WearUiState(
    val waterDone: Int = 0,
    val waterTarget: Int = HydrateMedsScheduleConfig.dailyWaterTarget,
    val morningDone: Boolean = false,
    val eveningDone: Boolean = false,
    val activeEvent: ReminderEvent? = null,
    val nextEvent: ReminderEvent? = null,
    val loggedIds: Set<String> = emptySet(),
)
