package com.zensa.app.ui.screens.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zensa.app.data.entity.NoteEntity
import com.zensa.app.data.preferences.TokenStorage
import com.zensa.app.data.repository.NoteRepository
import com.zensa.app.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotesViewModel(
    private val noteRepository: NoteRepository = ServiceLocator.noteRepository,
    private val tokenStorage: TokenStorage = ServiceLocator.tokenStorage()
) : ViewModel() {

    private val userId: String? get() = tokenStorage.getUserId()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val notes: StateFlow<List<NoteEntity>> =
        noteRepository.observeForUser(userId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val visibleNotes: StateFlow<List<NoteEntity>> =
        combine(notes, _searchQuery) { list, q ->
            if (q.isBlank()) list
            else list.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.content.contains(q, ignoreCase = true)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun refresh() {
        viewModelScope.launch { noteRepository.refresh() }
    }

    fun onSearchQueryChange(value: String) {
        _searchQuery.update { value }
    }

    fun delete(localId: String) {
        viewModelScope.launch { noteRepository.delete(localId) }
    }
}
