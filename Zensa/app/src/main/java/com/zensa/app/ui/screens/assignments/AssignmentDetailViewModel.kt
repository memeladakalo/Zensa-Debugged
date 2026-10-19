package com.zensa.app.ui.screens.assignments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zensa.app.core.network.ApiResult
import com.zensa.app.data.entity.AssignmentEntity
import com.zensa.app.data.entity.ModuleEntity
import com.zensa.app.data.entity.SubtaskEntity
import com.zensa.app.data.repository.AssignmentRepository
import com.zensa.app.data.repository.ModuleRepository
import com.zensa.app.data.repository.SubtaskRepository
import com.zensa.app.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * AssignmentDetailViewModel
 *
 * Powers the Assignment Details screen:
 *   - Shows one assignment's fields
 *   - Shows the module it belongs to (if any)
 *   - Lists its subtasks with completion toggles
 *   - Handles subtask add / toggle / delete
 *   - Handles assignment delete
 *
 * The subtasks are exposed as a reactive Flow from Room, so when
 * we toggle one, the UI updates instantly and the sync worker
 * (Step 9) will push the change to the server.
 */
class AssignmentDetailViewModel(
    private val assignmentRepository: AssignmentRepository =
        ServiceLocator.assignmentRepository,
    private val subtaskRepository: SubtaskRepository =
        ServiceLocator.subtaskRepository,
    private val moduleRepository: ModuleRepository =
        ServiceLocator.moduleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssignmentDetailUiState())
    val uiState: StateFlow<AssignmentDetailUiState> = _uiState.asStateFlow()

    private var assignmentLocalId: String? = null

    /**
     * Subtasks as a reactive stream. When [assignmentLocalId] is
     * known, this emits every time Room's subtasks table changes.
     */
    private val _subtasks = MutableStateFlow<List<SubtaskEntity>>(emptyList())
    val subtasks: StateFlow<List<SubtaskEntity>> = _subtasks.asStateFlow()

    // ---------- Lifecycle ----------

    fun start(localId: String) {
        assignmentLocalId = localId
        loadAssignment(localId)
        observeSubtasks(localId)
        // Best-effort refresh from the server (silent failure)
        viewModelScope.launch { subtaskRepository.refreshForAssignment(localId) }
    }

    private fun loadAssignment(localId: String) {
        viewModelScope.launch {
            val assignment = assignmentRepository.getByLocalId(localId)
            if (assignment == null) {
                _uiState.update { it.copy(generalError = "Assignment not found") }
                return@launch
            }
            val module = assignment.moduleLocalId?.let { moduleLocalId ->
                moduleRepository.observeModules(assignment.userId).first()
                    .firstOrNull { it.localId == moduleLocalId }
            }
            _uiState.update {
                it.copy(
                    assignment = assignment,
                    module = module,
                    generalError = null
                )
            }
        }
    }

    private fun observeSubtasks(localId: String) {
        viewModelScope.launch {
            subtaskRepository.observeSubtasks(localId).collect { list ->
                _subtasks.value = list
            }
        }
    }

    // ---------- Assignment actions ----------

    fun toggleAssignmentCompleted() {
        val assignment = _uiState.value.assignment ?: return
        viewModelScope.launch {
            assignmentRepository.setCompleted(
                assignment.localId,
                !assignment.isCompleted
            )
            // Reload to reflect the new state
            assignmentLocalId?.let { loadAssignment(it) }
        }
    }

    fun deleteAssignment(onDeleted: () -> Unit) {
        val assignment = _uiState.value.assignment ?: return
        viewModelScope.launch {
            assignmentRepository.delete(assignment.localId)
            onDeleted()
        }
    }

    // ---------- Subtask actions ----------

    fun addSubtask(title: String) {
        val id = assignmentLocalId ?: return
        if (title.isBlank()) return
        viewModelScope.launch {
            subtaskRepository.create(id, title)
        }
    }

    fun toggleSubtask(localId: String, completed: Boolean) {
        viewModelScope.launch {
            subtaskRepository.setCompleted(localId, completed)
        }
    }

    fun deleteSubtask(localId: String) {
        viewModelScope.launch {
            subtaskRepository.delete(localId)
        }
    }
}

data class AssignmentDetailUiState(
    val assignment: AssignmentEntity? = null,
    val module: ModuleEntity? = null,
    val generalError: String? = null
)
