package com.lazyjournal.app.ui.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyjournal.app.data.audio.AudioRecorder
import com.lazyjournal.app.data.repository.JournalRepository
import com.lazyjournal.app.data.transcription.TranscriptionService
import com.lazyjournal.app.data.transcription.WhisperModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

data class RecordUiState(
    val isRecording: Boolean = false,
    val elapsedSeconds: Long = 0,
    val status: String = "Ready",
    val error: String? = null,
    val lastEntryId: Long? = null,
    val isWhisperModelReady: Boolean = false,
    val whisperModelName: String = ""
)

class RecordViewModel(
    private val repository: JournalRepository,
    private val audioRecorder: AudioRecorder,
    private val transcriptionService: TranscriptionService,
    private val whisperModelManager: WhisperModelManager
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = mutableUiState.asStateFlow()

    private var timerJob: Job? = null
    private var recordingStartedAt: Long = 0L

    init {
        refreshModelState()
    }

    fun startRecording() {
        if (mutableUiState.value.isRecording) return

        runCatching {
            audioRecorder.start()
        }.onSuccess {
            recordingStartedAt = System.currentTimeMillis()
            mutableUiState.value = RecordUiState(
                isRecording = true,
                status = "Recording",
                isWhisperModelReady = whisperModelManager.hasDefaultModel(),
                whisperModelName = whisperModelManager.defaultModel.fileName
            )
            startTimer()
        }.onFailure { throwable ->
            mutableUiState.update {
                it.copy(
                    isRecording = false,
                    status = "Ready",
                    error = throwable.message ?: "Could not start recording."
                )
            }
        }
    }

    fun stopRecording() {
        if (!mutableUiState.value.isRecording) return

        timerJob?.cancel()
        mutableUiState.update {
            it.copy(status = "Saving")
        }

        viewModelScope.launch {
            runCatching {
                val file = withContext(Dispatchers.IO) {
                    audioRecorder.stop()
                }
                val entryId = repository.appendRecording(
                    audioFilePath = file.absolutePath,
                    createdAt = recordingStartedAt
                )
                entryId
            }.onSuccess { entryId ->
                mutableUiState.value = RecordUiState(
                    status = "Saved entry #$entryId. Transcribing locally.",
                    lastEntryId = entryId,
                    isWhisperModelReady = whisperModelManager.hasDefaultModel(),
                    whisperModelName = whisperModelManager.defaultModel.fileName
                )
                viewModelScope.launch {
                    transcriptionService.transcribeEntry(entryId)
                }
            }.onFailure { throwable ->
                mutableUiState.value = RecordUiState(
                    status = "Ready",
                    error = throwable.message ?: "Could not save recording."
                )
            }
        }
    }

    fun onPermissionDenied() {
        mutableUiState.update {
            it.copy(
                status = "Ready",
                error = "Microphone permission is required for recording."
            )
        }
    }

    fun clearError() {
        mutableUiState.update {
            it.copy(error = null)
        }
    }

    fun importWhisperModel(inputStream: InputStream?) {
        if (inputStream == null) {
            mutableUiState.update {
                it.copy(error = "Could not open selected model file.")
            }
            return
        }

        mutableUiState.update {
            it.copy(status = "Importing Whisper model")
        }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    inputStream.use { stream ->
                        whisperModelManager.importDefaultModel(stream)
                    }
                }
            }.onSuccess {
                mutableUiState.update {
                    it.copy(
                        status = "Whisper model ready",
                        isWhisperModelReady = true,
                        whisperModelName = whisperModelManager.defaultModel.fileName
                    )
                }
            }.onFailure { throwable ->
                mutableUiState.update {
                    it.copy(
                        status = "Ready",
                        error = throwable.message ?: "Could not import Whisper model."
                    )
                }
            }
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        if (mutableUiState.value.isRecording) {
            audioRecorder.cancel()
        }
        super.onCleared()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val elapsed = (System.currentTimeMillis() - recordingStartedAt) / 1000
                mutableUiState.update {
                    it.copy(elapsedSeconds = elapsed)
                }
                delay(500)
            }
        }
    }

    private fun refreshModelState() {
        mutableUiState.update {
            it.copy(
                isWhisperModelReady = whisperModelManager.hasDefaultModel(),
                whisperModelName = whisperModelManager.defaultModel.fileName
            )
        }
    }
}
