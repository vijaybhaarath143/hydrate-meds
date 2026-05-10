package dev.bhaarath.hydratemeds.shared.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ActiveReminderDao {
    @Query("SELECT * FROM active_reminders WHERE reminder_id = :reminderId LIMIT 1")
    suspend fun getByReminderId(reminderId: String): ActiveReminderEntity?

    @Query("SELECT * FROM active_reminders ORDER BY scheduled_at_epoch_millis ASC")
    fun observeActiveReminders(): Flow<List<ActiveReminderEntity>>

    @Query(
        """
        SELECT * FROM active_reminders
        WHERE scheduled_local_date = :localDate AND reminder_type = :reminderType
        """,
    )
    suspend fun getForLocalDateAndType(
        localDate: String,
        reminderType: String,
    ): List<ActiveReminderEntity>

    @Upsert
    suspend fun upsert(entity: ActiveReminderEntity)

    @Query("DELETE FROM active_reminders WHERE reminder_id = :reminderId")
    suspend fun clear(reminderId: String)

    @Query("DELETE FROM active_reminders")
    suspend fun clearAll()
}
