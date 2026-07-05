package com.lazyjournal.app.core

import android.content.Context
import com.lazyjournal.app.data.audio.AudioPlayer
import com.lazyjournal.app.data.audio.AudioRecorder
import com.lazyjournal.app.data.db.LazyJournalDatabase
import com.lazyjournal.app.data.repository.JournalRepository
import com.lazyjournal.app.data.transcription.TranscriptionService
import com.lazyjournal.app.data.transcription.WhisperCppTranscriber

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: LazyJournalDatabase by lazy {
        LazyJournalDatabase.getInstance(appContext)
    }

    val repository: JournalRepository by lazy {
        JournalRepository(database.journalEntryDao())
    }

    val audioRecorder: AudioRecorder by lazy {
        AudioRecorder(appContext)
    }

    val audioPlayer: AudioPlayer by lazy {
        AudioPlayer()
    }

    val transcriptionService: TranscriptionService by lazy {
        TranscriptionService(
            repository = repository,
            transcriber = WhisperCppTranscriber(appContext)
        )
    }
}
