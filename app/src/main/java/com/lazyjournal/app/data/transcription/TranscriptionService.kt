package com.lazyjournal.app.data.transcription

import com.lazyjournal.app.data.repository.JournalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TranscriptionService(
    private val repository: JournalRepository,
    private val transcriber: WhisperTranscriber
) {
    suspend fun transcribeEntry(entryId: Long) = withContext(Dispatchers.IO) {
        val entry = repository.getEntry(entryId) ?: return@withContext

        repository.markTranscriptRunning(entryId)
        runCatching {
            transcriber.transcribe(entry.audioFilePath)
        }.onSuccess { transcript ->
            repository.saveTranscript(entryId, transcript)
        }.onFailure { throwable ->
            repository.markTranscriptFailed(
                entryId = entryId,
                error = throwable.message ?: "Local transcription failed."
            )
        }
    }
}
