package com.lazyjournal.app.data.transcription

import android.content.Context
import java.io.File
import java.io.InputStream

class WhisperModelManager(context: Context) {
    private val modelsDir = File(context.filesDir, "models").apply {
        mkdirs()
    }

    val defaultModel: WhisperModel = WhisperModel(
        fileName = WhisperCppTranscriber.DEFAULT_MODEL_FILE,
        sourceUrl = WhisperCppTranscriber.DEFAULT_MODEL_SOURCE,
        file = File(modelsDir, WhisperCppTranscriber.DEFAULT_MODEL_FILE)
    )

    fun hasDefaultModel(): Boolean {
        return defaultModel.file.exists()
    }

    fun importDefaultModel(inputStream: InputStream) {
        defaultModel.file.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }
}

data class WhisperModel(
    val fileName: String,
    val sourceUrl: String,
    val file: File
)
