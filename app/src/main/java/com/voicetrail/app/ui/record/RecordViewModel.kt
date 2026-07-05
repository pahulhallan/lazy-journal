package com.voicetrail.app.ui.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicetrail.app.data.audio.AudioRecorder
import com.voicetrail.app.data.repository.JournalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RecordUiState(
    val isRecording: Boolean = false,
    val elapsedSeconds: Long = 0,
    val status: String = "Ready",
    val error: String? = null,
    val lastEntryId: Long? = null
)

class RecordViewModel(
    private val repository: JournalRepository,
    private val audioRecorder: AudioRecorder
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = mutableUiState.asStateFlow()

    private var timerJob: Job? = null
    private var recordingStartedAt: Long = 0L

    fun startRecording() {
        if (mutableUiState.value.isRecording) return

        runCatching {
            audioRecorder.start()
        }.onSuccess {
            recordingStartedAt = System.currentTimeMillis()
            mutableUiState.value = RecordUiState(
                isRecording = true,
                status = "Recording"
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
                    status = "Saved entry #$entryId",
                    lastEntryId = entryId
                )
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
}

