package com.lazyjournal.app.data.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class AudioRecorder(private val context: Context) {
    private var recorder: AudioRecord? = null
    private var activeFile: File? = null
    private var recordingThread: Thread? = null
    private val isRecording = AtomicBoolean(false)

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(): File {
        check(recorder == null) { "Recording is already active." }

        val outputFile = createOutputFile()
        val minBufferSize = AudioRecord.getMinBufferSize(
            SampleRate,
            ChannelConfig,
            AudioEncoding
        )
        check(minBufferSize > 0) { "Could not initialize microphone buffer." }

        val bufferSize = minBufferSize.coerceAtLeast(SampleRate / 2)
        val newRecorder = createRecorder(bufferSize)

        writeEmptyWavHeader(outputFile)
        newRecorder.startRecording()
        isRecording.set(true)

        recorder = newRecorder
        activeFile = outputFile
        recordingThread = thread(name = "lazy-journal-recorder") {
            writePcmToWav(outputFile, bufferSize)
        }

        return outputFile
    }

    fun stop(): File {
        val file = activeFile ?: error("No active recording file.")
        val currentRecorder = recorder ?: error("No active recorder.")

        isRecording.set(false)
        runCatching {
            currentRecorder.stop()
        }
        recordingThread?.join()
        currentRecorder.release()

        recorder = null
        recordingThread = null
        activeFile = null

        updateWavHeader(file)
        if (file.length() <= WavHeaderSize) {
            file.delete()
            error("Recording was empty.")
        }

        return file
    }

    fun cancel() {
        isRecording.set(false)
        recorder?.runCatching {
            stop()
            release()
        }
        recordingThread?.join()
        activeFile?.delete()
        recorder = null
        recordingThread = null
        activeFile = null
    }

    @SuppressLint("MissingPermission")
    private fun createRecorder(bufferSize: Int): AudioRecord {
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SampleRate,
            ChannelConfig,
            AudioEncoding,
            bufferSize
        )
        check(recorder.state == AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            "Could not initialize microphone."
        }
        return recorder
    }

    private fun writePcmToWav(outputFile: File, bufferSize: Int) {
        val currentRecorder = recorder ?: return
        val buffer = ShortArray(bufferSize / BytesPerSample)

        RandomAccessFile(outputFile, "rw").use { wavFile ->
            wavFile.seek(WavHeaderSize)
            while (isRecording.get()) {
                val samplesRead = currentRecorder.read(buffer, 0, buffer.size)
                if (samplesRead > 0) {
                    for (index in 0 until samplesRead) {
                        val sample = buffer[index].toInt()
                        wavFile.write(sample and 0xff)
                        wavFile.write((sample shr 8) and 0xff)
                    }
                }
            }
        }
    }

    private fun createOutputFile(): File {
        val recordingsDir = File(context.filesDir, "recordings").apply {
            mkdirs()
        }
        return File(recordingsDir, "lazy_journal_${System.currentTimeMillis()}.wav")
    }

    private fun writeEmptyWavHeader(file: File) {
        RandomAccessFile(file, "rw").use { wavFile ->
            wavFile.setLength(0)
            wavFile.write(ByteArray(WavHeaderSize.toInt()))
        }
    }

    private fun updateWavHeader(file: File) {
        val audioDataSize = file.length() - WavHeaderSize
        val byteRate = SampleRate * BytesPerSample

        RandomAccessFile(file, "rw").use { wavFile ->
            wavFile.seek(0)
            wavFile.writeAscii("RIFF")
            wavFile.writeLittleEndianInt((audioDataSize + 36).toInt())
            wavFile.writeAscii("WAVE")
            wavFile.writeAscii("fmt ")
            wavFile.writeLittleEndianInt(16)
            wavFile.writeLittleEndianShort(1)
            wavFile.writeLittleEndianShort(1)
            wavFile.writeLittleEndianInt(SampleRate)
            wavFile.writeLittleEndianInt(byteRate)
            wavFile.writeLittleEndianShort(BytesPerSample)
            wavFile.writeLittleEndianShort(16)
            wavFile.writeAscii("data")
            wavFile.writeLittleEndianInt(audioDataSize.toInt())
        }
    }

    private fun RandomAccessFile.writeAscii(value: String) {
        write(value.toByteArray(Charsets.US_ASCII))
    }

    private fun RandomAccessFile.writeLittleEndianInt(value: Int) {
        write(value and 0xff)
        write((value shr 8) and 0xff)
        write((value shr 16) and 0xff)
        write((value shr 24) and 0xff)
    }

    private fun RandomAccessFile.writeLittleEndianShort(value: Int) {
        write(value and 0xff)
        write((value shr 8) and 0xff)
    }

    private companion object {
        const val SampleRate = 16_000
        const val ChannelConfig = AudioFormat.CHANNEL_IN_MONO
        const val AudioEncoding = AudioFormat.ENCODING_PCM_16BIT
        const val BytesPerSample = 2
        const val WavHeaderSize = 44L
    }
}
