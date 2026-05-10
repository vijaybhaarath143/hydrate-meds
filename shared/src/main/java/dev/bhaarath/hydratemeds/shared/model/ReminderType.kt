package dev.bhaarath.hydratemeds.shared.model

enum class ReminderType(
    val wireName: String,
    val displayName: String,
) {
    Water("water", "Water"),
    MorningMedicine("morning_medicine", "Morning medicine"),
    EveningMedicine("evening_medicine", "Evening medicine");

    companion object {
        fun fromWireName(wireName: String): ReminderType =
            entries.first { it.wireName == wireName }
    }
}

