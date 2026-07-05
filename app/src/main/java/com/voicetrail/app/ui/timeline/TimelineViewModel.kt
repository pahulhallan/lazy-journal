package com.voicetrail.app.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicetrail.app.data.model.JournalEntry
import com.voicetrail.app.data.repository.JournalRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class TimelineViewModel(
    repository: JournalRepository
) : ViewModel() {
    val entries: StateFlow<List<JournalEntry>> = repository.observeEntries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}

