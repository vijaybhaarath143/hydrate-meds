package dev.bhaarath.hydratemeds.shared.model

import java.time.Instant
import java.time.LocalDate

data class AcknowledgmentLog(
    val reminderId: ReminderId,
    val reminderType: ReminderType,
    val scheduledAt: Instant,
    val scheduledLocalDate: LocalDate,
    val acknowledgedAt: Instant,
    val source: AcknowledgmentSource,
)

enum class AcknowledgmentSource(val wireName: String) {
    Phone("phone"),
    Watch("watch"),
    Sync("sync"),
    Unknown("unknown");

    companion object {
        fun fromWireName(wireName: String): AcknowledgmentSource =
            entries.firstOrNull { it.wireName == wireName } ?: Unknown
    }
}

