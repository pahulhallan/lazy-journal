package com.lazyjournal.app.data.transcription

import android.os.SystemClock
import android.util.Log
import com.lazyjournal.app.data.repository.JournalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TranscriptionService(
    private val repository: JournalRepository,
    private val transcriber: WhisperTranscriber
) {
    suspend fun transcribeEntry(entryId: Long) = withContext(Dispatchers.IO) {
        val entry = repository.getEntry(entryId) ?: return@withContext
        val startedAt = SystemClock.elapsedRealtime()

        Log.i(Tag, "Transcription started entryId=$entryId audio=${entry.audioFilePath}")
        repository.markTranscriptRunning(entryId)
        runCatching {
            transcriber.transcribe(entry.audioFilePath)
        }.onSuccess { transcript ->
            val elapsedMs = SystemClock.elapsedRealtime() - startedAt
            Log.i(
                Tag,
                "Transcription finished entryId=$entryId elapsedMs=$elapsedMs chars=${transcript.length}"
            )
            repository.saveTranscript(entryId, transcript)
        }.onFailure { throwable ->
            val elapsedMs = SystemClock.elapsedRealtime() - startedAt
            Log.e(
                Tag,
                "Transcription failed entryId=$entryId elapsedMs=$elapsedMs",
                throwable
            )
            repository.markTranscriptFailed(
                entryId = entryId,
                error = throwable.message ?: "Local transcription failed."
            )
        }
    }

    private companion object {
        const val Tag = "LazyJournalTranscription"
    }
}
