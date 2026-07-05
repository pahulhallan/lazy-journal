package com.lazyjournal.app.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var activeFile: File? = null

    fun start(): File {
        check(recorder == null) { "Recording is already active." }

        val outputFile = createOutputFile()
        val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        newRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }

        recorder = newRecorder
        activeFile = outputFile
        return outputFile
    }

    fun stop(): File {
        val file = activeFile ?: error("No active recording file.")
        val currentRecorder = recorder ?: error("No active recorder.")

        runCatching {
            currentRecorder.stop()
        }.onFailure {
            file.delete()
            throw it
        }

        currentRecorder.release()
        recorder = null
        activeFile = null
        return file
    }

    fun cancel() {
        recorder?.runCatching {
            stop()
            release()
        }
        activeFile?.delete()
        recorder = null
        activeFile = null
    }

    private fun createOutputFile(): File {
        val recordingsDir = File(context.filesDir, "recordings").apply {
            mkdirs()
        }
        return File(recordingsDir, "lazy_journal_${System.currentTimeMillis()}.m4a")
    }
}
