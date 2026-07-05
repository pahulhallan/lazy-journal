package com.voicetrail.app.core

import android.content.Context
import com.voicetrail.app.data.audio.AudioPlayer
import com.voicetrail.app.data.audio.AudioRecorder
import com.voicetrail.app.data.db.VoiceTrailDatabase
import com.voicetrail.app.data.repository.JournalRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: VoiceTrailDatabase by lazy {
        VoiceTrailDatabase.getInstance(appContext)
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
}

