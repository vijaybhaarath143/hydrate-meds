package dev.bhaarath.hydratemeds.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bhaarath.hydratemeds.shared.model.DailyReminderPause
import dev.bhaarath.hydratemeds.shared.model.AcknowledgmentLog
import dev.bhaarath.hydratemeds.shared.model.AcknowledgmentSource
import dev.bhaarath.hydratemeds.shared.model.ActiveReminder
import dev.bhaarath.hydratemeds.shared.model.ReminderEvent
import dev.bhaarath.hydratemeds.shared.model.ReminderType
import dev.bhaarath.hydratemeds.shared.model.TimelineState
import dev.bhaarath.hydratemeds.shared.repository.ReminderRepository
import dev.bhaarath.hydratemeds.shared.runtime.ReminderPauseCoordinator
import dev.bhaarath.hydratemeds.shared.schedule.HydrateMedsScheduleConfig
import dev.bhaarath.hydratemeds.shared.sync.ReminderDataLayerSync
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MobileHomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ReminderRepository,
    private val dataLayerSync: ReminderDataLayerSync,
) : ViewModel() {
    private val selectedDay = MutableStateFlow(LocalDate.now())
    private val now = MutableStateFlow(Instant.now())
    private val pauseOverrides = MutableStateFlow<Map<Pair<LocalDate, ReminderType>, Boolean>>(emptyMap())

    private val selectedLogs = selectedDay.flatMapLatest(repository::observeAcknowledgmentsForDate)
    private val storedSelectedPauses = selectedDay.flatMapLatest(repository::observePausedTypesForDate)
    private val selectedPauses = combine(
        selectedDay,
        storedSelectedPauses,
        pauseOverrides,
    ) { day, stored, overrides ->
        listOf(
            ReminderType.Water,
            ReminderType.MorningMedicine,
            ReminderType.EveningMedicine,
        ).fold(stored) { pauses, type ->
            when (overrides[day to type]) {
                true -> pauses + type
                false -> pauses - type
                null -> pauses
            }
        }
    }
    private val activeReminders = repository.observeActiveReminders()
    private val historyDays = lastDays(7)
    private val statsDays = lastDays(30)

    private val baseState = combine(
        selectedDay,
        selectedLogs,
        selectedPauses,
        activeReminders,
        historySummaries(historyDays),
    ) { day, logs, pauses, active, week ->
        MobileBaseState(day, logs, pauses, active, week)
    }

    val uiState = combine(
        baseState,
        historySummaries(statsDays),
        now,
    ) { base, month, instant ->
        val timeline = buildTimeline(
            events = repository.scheduledEventsForDate(base.day, ZoneId.systemDefault()),
            logs = base.logs,
            active = base.active,
            pausedTypes = base.pausedTypes,
            now = instant,
        )
        MobileUiState(
            selectedDay = base.day,
            timeline = timeline,
            week = base.week,
            stats = buildStats(month),
            waterDone = timeline.count { it.event.type == ReminderType.Water && it.log != null },
            waterTarget = HydrateMedsScheduleConfig.dailyWaterTarget,
            activeReminder = timeline.firstOrNull { it.state == TimelineState.Active },
            pausedTypes = base.pausedTypes,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MobileUiState.empty(),
    )

    fun selectDay(day: LocalDate) {
        selectedDay.value = day
        now.value = Instant.now()
    }

    fun logWater() {
        viewModelScope.launch {
            val state = uiState.value
            val now = Instant.now()
            val loggedIds = state.timeline.mapNotNullTo(mutableSetOf()) { it.log?.reminderId }
            val event = state.timeline
                .map { it.event }
                .lastOrNull {
                    it.type == ReminderType.Water &&
                        it.scheduledAt <= now &&
                        it.id !in loggedIds
                }
                ?: return@launch

            val log = repository.acknowledge(
                event = event,
                acknowledgedAt = Instant.now(),
                source = AcknowledgmentSource.Phone,
            )
            runCatching { dataLayerSync.sendAcknowledgment(log) }
        }
    }

    fun acknowledgeActive() {
        viewModelScope.launch {
            val item = uiState.value.activeReminder ?: return@launch
            val log = repository.acknowledge(
                event = item.event,
                acknowledgedAt = Instant.now(),
                source = AcknowledgmentSource.Phone,
            )
            runCatching { dataLayerSync.sendAcknowledgment(log) }
        }
    }

    fun togglePause(reminderType: ReminderType) {
        val state = uiState.value
        val paused = reminderType !in state.pausedTypes
        val key = state.selectedDay to reminderType
        pauseOverrides.update { it + (key to paused) }

        viewModelScope.launch {
            try {
                ReminderPauseCoordinator.setPaused(
                    context = context,
                    date = state.selectedDay,
                    reminderType = reminderType,
                    paused = paused,
                )
            } catch (_: Exception) {
                pauseOverrides.update { it - key }
                return@launch
            }

            runCatching {
                dataLayerSync.sendDailyPause(
                    DailyReminderPause(
                        date = state.selectedDay,
                        reminderType = reminderType,
                        paused = paused,
                    ),
                )
            }
        }
    }

    private fun historySummaries(days: List<LocalDate>) =
        combine(days.map { day ->
            repository.observeAcknowledgmentsForDate(day).map { logs ->
                DaySummary(
                    date = day,
                    completed = logs.distinctBy { it.reminderId }.size,
                    total = HydrateMedsScheduleConfig.dailyReminderCount,
                    waterCompleted = logs.count { it.reminderType == ReminderType.Water },
                )
            }
        }) { summaries -> summaries.toList() }

    private fun buildTimeline(
        events: List<ReminderEvent>,
        logs: List<AcknowledgmentLog>,
        active: List<ActiveReminder>,
        pausedTypes: Set<ReminderType>,
        now: Instant,
    ): List<TimelineItem> {
        val logsById = logs.associateBy { it.reminderId }
        val activeIds = active.mapTo(mutableSetOf()) { it.reminderId }

        return events.map { event ->
            val log = logsById[event.id]
            val lateness = log?.let { Duration.between(event.scheduledAt, it.acknowledgedAt) }
            val reminderWindowEndsAt = event.scheduledAt.plus(
                HydrateMedsScheduleConfig.nagWindow(event.type),
            )
            val state = when {
                log != null && (lateness == null || lateness <= event.onTimeWindow) ->
                    TimelineState.DoneOnTime
                log != null -> TimelineState.DoneLate
                event.type in pausedTypes -> TimelineState.Paused
                event.id in activeIds || (now >= event.scheduledAt && now < reminderWindowEndsAt) ->
                    TimelineState.Active
                now >= reminderWindowEndsAt -> TimelineState.Missed
                else -> TimelineState.Upcoming
            }
            TimelineItem(
                event = event,
                log = log,
                state = state,
                latenessMinutes = lateness?.toMinutes()?.coerceAtLeast(0),
            )
        }
    }

    private fun buildStats(month: List<DaySummary>): StatsSummary {
        val sorted = month.sortedByDescending { it.date }
        val currentStreak = sorted.takeWhile {
            it.waterCompleted == HydrateMedsScheduleConfig.dailyWaterTarget
        }.size
        var longest = 0
        var running = 0
        month.sortedBy { it.date }.forEach {
            if (it.waterCompleted == HydrateMedsScheduleConfig.dailyWaterTarget) {
                running += 1
                longest = maxOf(longest, running)
            } else {
                running = 0
            }
        }
        return StatsSummary(
            currentWaterStreak = currentStreak,
            longestWaterStreak = longest,
            month = month,
        )
    }

    private fun lastDays(count: Int): List<LocalDate> {
        val today = LocalDate.now()
        return (count - 1 downTo 0).map { today.minusDays(it.toLong()) }
    }
}

data class MobileUiState(
    val selectedDay: LocalDate,
    val timeline: List<TimelineItem>,
    val week: List<DaySummary>,
    val stats: StatsSummary,
    val waterDone: Int,
    val waterTarget: Int,
    val activeReminder: TimelineItem?,
    val pausedTypes: Set<ReminderType>,
) {
    companion object {
        fun empty(): MobileUiState =
            MobileUiState(
                selectedDay = LocalDate.now(),
                timeline = emptyList(),
                week = emptyList(),
                stats = StatsSummary(),
                waterDone = 0,
                waterTarget = HydrateMedsScheduleConfig.dailyWaterTarget,
                activeReminder = null,
                pausedTypes = emptySet(),
            )
    }
}

private data class MobileBaseState(
    val day: LocalDate,
    val logs: List<AcknowledgmentLog>,
    val pausedTypes: Set<ReminderType>,
    val active: List<ActiveReminder>,
    val week: List<DaySummary>,
)

data class TimelineItem(
    val event: ReminderEvent,
    val log: AcknowledgmentLog?,
    val state: TimelineState,
    val latenessMinutes: Long?,
)

data class DaySummary(
    val date: LocalDate,
    val completed: Int,
    val total: Int,
    val waterCompleted: Int,
) {
    val completion: Float = if (total == 0) 0f else completed / total.toFloat()
}

data class StatsSummary(
    val currentWaterStreak: Int = 0,
    val longestWaterStreak: Int = 0,
    val month: List<DaySummary> = emptyList(),
)

val compactDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
