package com.zensa.app.ui.screens.assignments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zensa.app.core.network.ApiResult
import com.zensa.app.data.entity.ModuleEntity
import com.zensa.app.data.preferences.TokenStorage
import com.zensa.app.data.repository.AssignmentRepository
import com.zensa.app.data.repository.ModuleRepository
import com.zensa.app.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * AssignmentFormViewModel
 *
 * Used by AssignmentFormScreen for both CREATE and EDIT.
 *
 * start(localId):
 *   - null     → CREATE mode; load modules; reset form.
 *   - non-null → EDIT mode; load the assignment from Room and
 *                pre-fill the form fields.
 *
 * save():
 *   - Validates the form
 *   - CREATE → assignmentRepository.create(...)
 *   - EDIT   → assignmentRepository.update(...)
 *   - Invokes onSaved() on success
 */
class AssignmentFormViewModel(
    private val assignmentRepository: AssignmentRepository =
        ServiceLocator.assignmentRepository,
    private val moduleRepository: ModuleRepository =
        ServiceLocator.moduleRepository,
    private val tokenStorage: TokenStorage =
        ServiceLocator.tokenStorage()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssignmentFormUiState())
    val uiState: StateFlow<AssignmentFormUiState> = _uiState.asStateFlow()

    /** Local id of the assignment being edited, or null for create. */
    private var editingLocalId: String? = null

    // ---------- Lifecycle ----------

    fun start(localId: String?) {
    editingLocalId = localId
    loadModules()

    if (localId == null) {
        // CREATE mode — pre-select the user's default priority
        // from DataStore. Read once, on the coroutine that fills
        // in the modules anyway.
        viewModelScope.launch {
            val defaultPriority = ServiceLocator
                .userPreferences()
                .defaultPriority
                .first()
            _uiState.update {
                it.copy(
                    title = "",
                    description = "",
                    dueDate = "",
                    priority = defaultPriority,
                    moduleLocalId = null,
                    titleError = null,
                    dueDateError = null,
                    generalError = null,
                    isLoading = false
                )
            }
        }
    } else {
            // EDIT mode — load existing
            viewModelScope.launch {
                val existing = assignmentRepository.getByLocalId(localId)
                if (existing == null) {
                    _uiState.update { it.copy(generalError = "Assignment not found") }
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        title = existing.title,
                        description = existing.description,
                        dueDate = existing.dueDate.substringBefore("T"),
                        priority = existing.priority,
                        moduleLocalId = existing.moduleLocalId,
                        titleError = null,
                        dueDateError = null,
                        generalError = null
                    )
                }
            }
        }
    }

    // ---------- Input handling ----------

    fun onTitleChange(v: String) =
        _uiState.update { it.copy(title = v, titleError = null, generalError = null) }

    fun onDescriptionChange(v: String) =
        _uiState.update { it.copy(description = v) }

    fun onDueDateChange(v: String) =
        _uiState.update { it.copy(dueDate = v, dueDateError = null, generalError = null) }

    fun onPriorityChange(v: String) =
        _uiState.update { it.copy(priority = v) }

    fun onModuleChange(moduleLocalId: String?) =
        _uiState.update { it.copy(moduleLocalId = moduleLocalId) }

    // ---------- Save ----------

    fun save(onSuccess: () -> Unit) {
        val s = _uiState.value

        val titleError = if (s.title.isBlank()) "Title is required" else null
        val dueDateError = validateDueDate(s.dueDate)

        if (titleError != null || dueDateError != null) {
            _uiState.update {
                it.copy(titleError = titleError, dueDateError = dueDateError)
            }
            return
        }

        val userId = tokenStorage.getUserId()
        // Normalise: form takes "YYYY-MM-DD"; store as ISO datetime.
        val isoDueDate = if (s.dueDate.contains("T")) s.dueDate
        else "${s.dueDate}T23:59:00"

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }

            val result = if (editingLocalId == null) {
                assignmentRepository.create(
                    userId = userId,
                    moduleLocalId = s.moduleLocalId,
                    title = s.title,
                    description = s.description,
                    priority = s.priority,
                    status = "PENDING",
                    dueDate = isoDueDate
                )
            } else {
                val existing = assignmentRepository.getByLocalId(editingLocalId!!)
                assignmentRepository.update(
                    localId = editingLocalId!!,
                    moduleLocalId = s.moduleLocalId,
                    title = s.title,
                    description = s.description,
                    priority = s.priority,
                    status = existing?.status ?: "PENDING",
                    dueDate = isoDueDate,
                    isCompleted = existing?.isCompleted ?: false
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

    // ---------- Internals ----------

    private fun loadModules() {
        viewModelScope.launch {
            val userId = tokenStorage.getUserId()
            val modules: List<ModuleEntity> =
                moduleRepository.observeModules(userId).first()
            _uiState.update { it.copy(availableModules = modules) }
        }
    }

    /**
     * Accepts YYYY-MM-DD. Rejects anything else with a friendly
     * message. (We will upgrade to a date picker later, but this
     * keeps the form usable today and is trivially unit-testable.)
     */
    private fun validateDueDate(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return "Due date is required"
        val parts = trimmed.substringBefore("T").split("-")
        if (parts.size != 3) return "Use format YYYY-MM-DD"
        val year = parts[0].toIntOrNull()
        val month = parts[1].toIntOrNull()
        val day = parts[2].toIntOrNull()
        if (year == null || month == null || day == null) return "Use format YYYY-MM-DD"
        if (month !in 1..12) return "Month must be between 1 and 12"
        if (day !in 1..31) return "Day must be between 1 and 31"
        return null
    }
}

data class AssignmentFormUiState(
    val title: String = "",
    val description: String = "",
    val dueDate: String = "",
    val priority: String = "MEDIUM",
    val moduleLocalId: String? = null,

    val titleError: String? = null,
    val dueDateError: String? = null,
    val generalError: String? = null,

    val isLoading: Boolean = false,
    val availableModules: List<ModuleEntity> = emptyList()
)
