package dev.bhaarath.hydratemeds.shared.model

import java.time.Instant

@JvmInline
value class ReminderId(val value: String) {
    companion object {
        fun forScheduled(type: ReminderType, scheduledAt: Instant): ReminderId =
            ReminderId("${type.wireName}:${scheduledAt.toEpochMilli()}")
    }
}

