package com.lazyjournal.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lazyjournal.app.data.model.JournalEntry
import com.lazyjournal.app.data.repository.JournalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class SearchUiState(
    val query: String = "",
    val results: List<JournalEntry> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val repository: JournalRepository
) : ViewModel() {
    private val query = MutableStateFlow("")

    val uiState: StateFlow<SearchUiState> = query
        .flatMapLatest { currentQuery ->
            repository.searchEntries(currentQuery).map { results ->
                SearchUiState(
                    query = currentQuery,
                    results = results
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SearchUiState()
        )

    fun onQueryChange(value: String) {
        query.value = value
    }
}
