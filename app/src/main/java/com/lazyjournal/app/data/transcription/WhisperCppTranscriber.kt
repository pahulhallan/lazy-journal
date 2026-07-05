package com.lazyjournal.app.data.transcription

import android.content.Context

class WhisperCppTranscriber(
    context: Context,
) : WhisperTranscriber {
    private val modelManager = WhisperModelManager(context)

    override fun transcribe(audioFilePath: String): String {
        val model = modelManager.defaultModel
        if (!modelManager.hasDefaultModel()) {
            error(
                "Whisper model not found. Import ${model.fileName} from ${model.sourceUrl} before transcribing."
            )
        }
        if (!nativeLibraryAvailable) {
            error("whisper.cpp native library is not bundled yet.")
        }

        return nativeTranscribe(
            modelPath = model.file.absolutePath,
            audioPath = audioFilePath
        )
    }

    private external fun nativeTranscribe(
        modelPath: String,
        audioPath: String
    ): String

    companion object {
        const val DEFAULT_MODEL_FILE = "ggml-tiny.en.bin"
        const val DEFAULT_MODEL_SOURCE = "https://huggingface.co/ggerganov/whisper.cpp"

        private val nativeLibraryAvailable: Boolean = runCatching {
            System.loadLibrary("lazyjournal_whisper")
        }.isSuccess
    }
}
