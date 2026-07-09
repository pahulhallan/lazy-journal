package com.lazyjournal.app.data.repository

import com.lazyjournal.app.data.db.JournalEntryDao
import com.lazyjournal.app.data.db.JournalEntryEntity
import com.lazyjournal.app.data.model.JournalEntry
import com.lazyjournal.app.data.model.TranscriptStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

class JournalRepository(
    private val dao: JournalEntryDao
) {
    fun observeEntries(): Flow<List<JournalEntry>> {
        return dao.observeEntries().map { entries ->
            entries.map { it.toModel() }
        }
    }

    fun observeEntry(id: Long): Flow<JournalEntry?> {
        return dao.observeEntry(id).map { it?.toModel() }
    }

    fun searchEntries(query: String): Flow<List<JournalEntry>> {
        val trimmed = query.trim()
        return if (trimmed.isBlank()) {
            observeEntries()
        } else {
            dao.searchEntries(trimmed).map { entries ->
                entries.map { it.toModel() }
            }
        }
    }

    suspend fun appendRecording(
        audioFilePath: String,
        createdAt: Long = System.currentTimeMillis(),
        latitude: Double? = null,
        longitude: Double? = null,
        locationLabel: String? = null,
        tags: List<String> = emptyList()
    ): Long {
        return dao.insert(
            JournalEntryEntity(
                createdAt = createdAt,
                transcript = "",
                latitude = latitude,
                longitude = longitude,
                locationLabel = locationLabel,
                audioFilePath = audioFilePath,
                tags = encodeTags(tags),
                transcriptStatus = TranscriptStatus.Pending.storageValue
            )
        )
    }

    suspend fun getEntry(id: Long): JournalEntry? {
        return dao.getEntry(id)?.toModel()
    }

    suspend fun markTranscriptQueued(entryId: Long) {
        dao.updateTranscriptStatus(
            entryId = entryId,
            status = TranscriptStatus.Queued.storageValue
        )
    }

    suspend fun markTranscriptRunning(entryId: Long) {
        dao.updateTranscriptStatus(
            entryId = entryId,
            status = TranscriptStatus.Running.storageValue
        )
    }

    suspend fun saveTranscript(entryId: Long, transcript: String) {
        dao.updateTranscript(
            entryId = entryId,
            transcript = transcript.trim(),
            status = TranscriptStatus.Complete.storageValue
        )
    }

    suspend fun markTranscriptFailed(entryId: Long, error: String) {
        dao.updateTranscriptStatus(
            entryId = entryId,
            status = TranscriptStatus.Failed.storageValue,
            error = error
        )
    }

    private fun JournalEntryEntity.toModel(): JournalEntry {
        return JournalEntry(
            id = id,
            createdAt = createdAt,
            transcript = transcript,
            latitude = latitude,
            longitude = longitude,
            locationLabel = locationLabel,
            audioFilePath = audioFilePath,
            tags = decodeTags(tags),
            transcriptStatus = TranscriptStatus.fromStorageValue(transcriptStatus),
            transcriptError = transcriptError
        )
    }

    private fun encodeTags(tags: List<String>): String {
        val array = JSONArray()
        tags.forEach { array.put(it) }
        return array.toString()
    }

    private fun decodeTags(raw: String): List<String> {
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    add(array.getString(index))
                }
            }
        }.getOrDefault(emptyList())
    }
}
