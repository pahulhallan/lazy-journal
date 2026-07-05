package com.lazyjournal.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lazyjournal.app.core.AppContainer
import com.lazyjournal.app.ui.record.RecordViewModel
import com.lazyjournal.app.ui.search.SearchViewModel
import com.lazyjournal.app.ui.timeline.TimelineViewModel

class AppViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(RecordViewModel::class.java) -> {
                RecordViewModel(
                    repository = container.repository,
                    audioRecorder = container.audioRecorder
                ) as T
            }

            modelClass.isAssignableFrom(TimelineViewModel::class.java) -> {
                TimelineViewModel(container.repository) as T
            }

            modelClass.isAssignableFrom(SearchViewModel::class.java) -> {
                SearchViewModel(container.repository) as T
            }

            else -> error("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
