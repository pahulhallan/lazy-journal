package com.voicetrail.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalEntryDao {
    @Insert
    suspend fun insert(entry: JournalEntryEntity): Long

    @Query("SELECT * FROM journal_entries ORDER BY created_at DESC")
    fun observeEntries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    fun observeEntry(id: Long): Flow<JournalEntryEntity?>

    @Query(
        """
        SELECT * FROM journal_entries
        WHERE transcript LIKE '%' || :query || '%'
            OR tags LIKE '%' || :query || '%'
            OR location_label LIKE '%' || :query || '%'
        ORDER BY created_at DESC
        """
    )
    fun searchEntries(query: String): Flow<List<JournalEntryEntity>>
}

