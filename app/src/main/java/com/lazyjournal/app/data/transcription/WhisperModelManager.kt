package com.lazyjournal.app.data.transcription

import android.content.Context
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream

class WhisperModelManager(context: Context) {
    private val assetManager = context.assets
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

    fun ensureDefaultModelAvailable(): Boolean {
        if (hasDefaultModel()) {
            Log.i(Tag, "Whisper model ready path=${defaultModel.file.absolutePath}")
            return true
        }

        return runCatching {
            val startedAt = SystemClock.elapsedRealtime()
            Log.i(Tag, "Bundled Whisper model copy started asset=$BundledModelAssetPath")
            assetManager.open(BundledModelAssetPath).use { inputStream ->
                importDefaultModel(inputStream)
            }
            val elapsedMs = SystemClock.elapsedRealtime() - startedAt
            Log.i(
                Tag,
                "Bundled Whisper model copy finished elapsedMs=$elapsedMs bytes=${defaultModel.file.length()}"
            )
            hasDefaultModel()
        }.getOrElse { throwable ->
            if (throwable is FileNotFoundException) {
                Log.w(Tag, "Bundled Whisper model asset missing: $BundledModelAssetPath")
                false
            } else {
                Log.e(Tag, "Could not prepare bundled Whisper model.", throwable)
                throw throwable
            }
        }
    }

    fun importDefaultModel(inputStream: InputStream) {
        val startedAt = SystemClock.elapsedRealtime()
        val tempFile = File(modelsDir, "${defaultModel.fileName}.tmp")
        tempFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        if (tempFile.length() <= 0L) {
            tempFile.delete()
            error("Whisper model file is empty.")
        }
        if (defaultModel.file.exists()) {
            defaultModel.file.delete()
        }
        check(tempFile.renameTo(defaultModel.file)) {
            tempFile.delete()
            "Could not save Whisper model."
        }
        val elapsedMs = SystemClock.elapsedRealtime() - startedAt
        Log.i(
            Tag,
            "Whisper model saved elapsedMs=$elapsedMs bytes=${defaultModel.file.length()}"
        )
    }

    private companion object {
        const val Tag = "LazyJournalModel"
        const val BundledModelAssetPath = "models/${WhisperCppTranscriber.DEFAULT_MODEL_FILE}"
    }
}

data class WhisperModel(
    val fileName: String,
    val sourceUrl: String,
    val file: File
)
