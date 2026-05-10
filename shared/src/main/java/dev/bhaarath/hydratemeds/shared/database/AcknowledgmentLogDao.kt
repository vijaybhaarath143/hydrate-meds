package dev.bhaarath.hydratemeds.shared.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AcknowledgmentLogDao {
    @Query("SELECT * FROM acknowledgment_logs WHERE reminder_id = :reminderId LIMIT 1")
    suspend fun getByReminderId(reminderId: String): AcknowledgmentLogEntity?

    @Query(
        """
        SELECT * FROM acknowledgment_logs
        WHERE scheduled_local_date = :localDateIso
        ORDER BY scheduled_at_epoch_millis ASC
        """,
    )
    fun observeForLocalDate(localDateIso: String): Flow<List<AcknowledgmentLogEntity>>

    @Query(
        """
        SELECT * FROM acknowledgment_logs
        WHERE scheduled_local_date = :localDateIso
        ORDER BY scheduled_at_epoch_millis ASC
        """,
    )
    suspend fun getForLocalDate(localDateIso: String): List<AcknowledgmentLogEntity>

    @Query(
        """
        SELECT * FROM acknowledgment_logs
        WHERE scheduled_at_epoch_millis >= :startEpochMillis
          AND scheduled_at_epoch_millis < :endEpochMillis
        ORDER BY scheduled_at_epoch_millis ASC
        """,
    )
    fun observeBetween(
        startEpochMillis: Long,
        endEpochMillis: Long,
    ): Flow<List<AcknowledgmentLogEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: AcknowledgmentLogEntity): Long

    @Query(
        """
        UPDATE acknowledgment_logs
        SET acknowledged_at_epoch_millis = CASE
                WHEN :acknowledgedAtEpochMillis < acknowledged_at_epoch_millis
                THEN :acknowledgedAtEpochMillis
                ELSE acknowledged_at_epoch_millis
            END,
            source = CASE
                WHEN :acknowledgedAtEpochMillis < acknowledged_at_epoch_millis
                THEN :source
                ELSE source
            END,
            updated_at_epoch_millis = :updatedAtEpochMillis
        WHERE reminder_id = :reminderId
        """,
    )
    suspend fun keepEarliestAcknowledgment(
        reminderId: String,
        acknowledgedAtEpochMillis: Long,
        source: String,
        updatedAtEpochMillis: Long,
    )

    @Transaction
    suspend fun upsertEarliest(entity: AcknowledgmentLogEntity) {
        val insertedRowId = insertIgnore(entity)
        if (insertedRowId == -1L) {
            keepEarliestAcknowledgment(
                reminderId = entity.reminderId,
                acknowledgedAtEpochMillis = entity.acknowledgedAtEpochMillis,
                source = entity.source,
                updatedAtEpochMillis = entity.updatedAtEpochMillis,
            )
        }
    }
}
