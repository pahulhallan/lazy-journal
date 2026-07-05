package com.lazyjournal.app.data.transcription

interface WhisperTranscriber {
    fun transcribe(audioFilePath: String): String
}
