package com.lazyjournal.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lazyjournal.app.data.audio.AudioPlayer
import com.lazyjournal.app.data.model.JournalEntry
import com.lazyjournal.app.data.repository.JournalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class EntryDetailUiState(
    val entry: JournalEntry? = null,
    val isPlaying: Boolean = false,
    val error: String? = null
)

class EntryDetailViewModel(
    entryId: Long,
    private val repository: JournalRepository,
    private val audioPlayer: AudioPlayer
) : ViewModel() {
    private val isPlaying = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<EntryDetailUiState> = combine(
        repository.observeEntry(entryId),
        isPlaying,
        error
    ) { entry, currentlyPlaying, currentError ->
        EntryDetailUiState(
            entry = entry,
            isPlaying = currentlyPlaying,
            error = currentError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EntryDetailUiState()
    )

    fun togglePlayback() {
        val entry = uiState.value.entry ?: return
        if (uiState.value.isPlaying) {
            stopPlayback()
            return
        }

        runCatching {
            audioPlayer.play(entry.audioFilePath) {
                isPlaying.value = false
            }
        }.onSuccess {
            isPlaying.value = true
        }.onFailure { throwable ->
            isPlaying.value = false
            error.value = throwable.message ?: "Could not play recording."
        }
    }

    fun clearError() {
        error.value = null
    }

    override fun onCleared() {
        stopPlayback()
        super.onCleared()
    }

    private fun stopPlayback() {
        audioPlayer.stop()
        isPlaying.update { false }
    }
}

class EntryDetailViewModelFactory(
    private val entryId: Long,
    private val repository: JournalRepository,
    private val audioPlayer: AudioPlayer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EntryDetailViewModel::class.java)) {
            return EntryDetailViewModel(
                entryId = entryId,
                repository = repository,
                audioPlayer = audioPlayer
            ) as T
        }
        error("Unknown ViewModel class: ${modelClass.name}")
    }
}
