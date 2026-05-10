package dev.bhaarath.hydratemeds.shared.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyReminderPauseDao {
    @Query("SELECT * FROM daily_reminder_pauses WHERE local_date = :localDate")
    fun observeForLocalDate(localDate: String): Flow<List<DailyReminderPauseEntity>>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM daily_reminder_pauses
            WHERE local_date = :localDate AND reminder_type = :reminderType
        )
        """,
    )
    suspend fun isPaused(localDate: String, reminderType: String): Boolean

    @Upsert
    suspend fun upsert(entity: DailyReminderPauseEntity)

    @Query("DELETE FROM daily_reminder_pauses WHERE local_date = :localDate AND reminder_type = :reminderType")
    suspend fun delete(localDate: String, reminderType: String)
}
