package com.zensa.app.ui.screens.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zensa.app.core.network.ApiResult
import com.zensa.app.data.entity.ModuleEntity
import com.zensa.app.data.preferences.TokenStorage
import com.zensa.app.data.repository.ModuleRepository
import com.zensa.app.data.repository.NoteRepository
import com.zensa.app.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NoteFormViewModel(
    private val noteRepository: NoteRepository = ServiceLocator.noteRepository,
    private val moduleRepository: ModuleRepository = ServiceLocator.moduleRepository,
    private val tokenStorage: TokenStorage = ServiceLocator.tokenStorage()
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteFormUiState())
    val uiState: StateFlow<NoteFormUiState> = _uiState.asStateFlow()

    private var editingLocalId: String? = null

    fun start(localId: String?) {
        editingLocalId = localId
        loadModules()

        if (localId == null) {
            _uiState.update {
                it.copy(
                    title = "",
                    content = "",
                    moduleLocalId = null,
                    titleError = null,
                    generalError = null,
                    isLoading = false
                )
            }
        } else {
            viewModelScope.launch {
                val existing = noteRepository.getByLocalId(localId)
                if (existing == null) {
                    _uiState.update { it.copy(generalError = "Note not found") }
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        title = existing.title,
                        content = existing.content,
                        moduleLocalId = existing.moduleLocalId,
                        titleError = null,
                        generalError = null
                    )
                }
            }
        }
    }

    fun onTitleChange(v: String) =
        _uiState.update { it.copy(title = v, titleError = null, generalError = null) }

    fun onContentChange(v: String) =
        _uiState.update { it.copy(content = v) }

    fun onModuleChange(moduleLocalId: String?) =
        _uiState.update { it.copy(moduleLocalId = moduleLocalId) }

    fun save(onSuccess: () -> Unit) {
        val s = _uiState.value
        if (s.title.isBlank() && s.content.isBlank()) {
            _uiState.update { it.copy(titleError = "Write a title or some content") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }

            val result = if (editingLocalId == null) {
                noteRepository.create(
                    userId = tokenStorage.getUserId(),
                    moduleLocalId = s.moduleLocalId,
                    title = s.title,
                    content = s.content
                )
            } else {
                noteRepository.update(
                    localId = editingLocalId!!,
                    moduleLocalId = s.moduleLocalId,
                    title = s.title,
                    content = s.content
                )
            }

            when (result) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, generalError = result.message)
                    }
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val id = editingLocalId ?: return
        viewModelScope.launch {
            noteRepository.delete(id)
            onDeleted()
        }
    }

    private fun loadModules() {
        viewModelScope.launch {
            val modules: List<ModuleEntity> =
                moduleRepository.observeModules(tokenStorage.getUserId()).first()
            _uiState.update { it.copy(availableModules = modules) }
        }
    }
}

data class NoteFormUiState(
    val title: String = "",
    val content: String = "",
    val moduleLocalId: String? = null,
    val titleError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
    val availableModules: List<ModuleEntity> = emptyList()
)
