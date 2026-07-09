package com.lazyjournal.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalEntryDao {
    @Insert
    suspend fun insert(entry: JournalEntryEntity): Long

    @Query("SELECT * FROM journal_entries ORDER BY created_at DESC")
    fun observeEntries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    fun observeEntry(id: Long): Flow<JournalEntryEntity?>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getEntry(id: Long): JournalEntryEntity?

    @Query(
        """
        UPDATE journal_entries
        SET transcript_status = :status,
            transcript_error = :error
        WHERE id = :entryId
        """
    )
    suspend fun updateTranscriptStatus(
        entryId: Long,
        status: String,
        error: String? = null
    )

    @Query(
        """
        UPDATE journal_entries
        SET transcript = :transcript,
            transcript_status = :status,
            transcript_error = NULL
        WHERE id = :entryId
        """
    )
    suspend fun updateTranscript(
        entryId: Long,
        transcript: String,
        status: String = "complete"
    )

    @RawQuery(observedEntities = [JournalEntryEntity::class])
    fun searchEntries(query: SupportSQLiteQuery): Flow<List<JournalEntryEntity>>
}
