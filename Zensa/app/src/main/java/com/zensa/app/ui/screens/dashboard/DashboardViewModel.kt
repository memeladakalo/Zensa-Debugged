package com.zensa.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zensa.app.data.entity.AssignmentEntity
import com.zensa.app.data.entity.SubtaskEntity
import com.zensa.app.data.preferences.TokenStorage
import com.zensa.app.data.repository.AssignmentRepository
import com.zensa.app.data.repository.SubtaskRepository
import com.zensa.app.di.ServiceLocator
import com.zensa.app.domain.usecase.FocusedAssignment
import com.zensa.app.domain.usecase.SmartFocusUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * DashboardViewModel
 *
 * Provides the dashboard with:
 *   - The current user's name, email, auth status
 *   - Smart Focus: the top-N highest-scoring assignments
 *
 * Smart Focus is recomputed every time the assignments or subtasks
 * change (Room emits a new list), so the dashboard stays live
 * without any manual refresh.
 */
class DashboardViewModel(
    private val tokenStorage: TokenStorage = ServiceLocator.tokenStorage(),
    private val assignmentRepository: AssignmentRepository =
        ServiceLocator.assignmentRepository,
    private val subtaskRepository: SubtaskRepository =
        ServiceLocator.subtaskRepository,
    private val smartFocus: SmartFocusUseCase = SmartFocusUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            name = tokenStorage.getUserName().orEmpty(),
            email = tokenStorage.getUserEmail().orEmpty(),
            isAuthenticated = tokenStorage.hasToken()
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeFocus()
    }

    // ---------- Smart Focus pipeline ----------

    /**
     * Subscribes to assignments in Room. Whenever the list changes,
     * reloads subtasks for each assignment, then runs Smart Focus.
     *
     * This is deliberately simple: the number of assignments on a
     * student device is small (tens, not thousands), so doing a
     * small read per assignment is fine here. If it ever becomes
     * a problem, we swap to a Room query that preloads subtasks.
     */
    private fun observeFocus() {
        viewModelScope.launch {
            val userId = tokenStorage.getUserId()
            assignmentRepository.observeAssignments(userId).collect { assignments ->
                val subtasksByAssignment: Map<String, List<SubtaskEntity>> =
                    assignments.associate { a ->
                        a.localId to subtaskRepository.getForAssignment(a.localId)
                    }
                val focused: List<FocusedAssignment> = smartFocus.rank(
                    assignments = assignments,
                    subtasksByAssignment = subtasksByAssignment,
                    limit = SmartFocusUseCase.DEFAULT_LIMIT
                )
                _uiState.update { it.copy(focus = focused) }
            }
        }
    }

    // ---------- Actions ----------

    /** Best-effort refresh of assignments from the API. */
    fun refresh() {
        viewModelScope.launch {
            assignmentRepository.refresh()
        }
    }

    fun toggleFocusCompleted(localId: String, completed: Boolean) {
        viewModelScope.launch {
            assignmentRepository.setCompleted(localId, completed)
        }
    }

    fun logout() {
        tokenStorage.clear()
    }
}

data class DashboardUiState(
    val name: String = "",
    val email: String = "",
    val isAuthenticated: Boolean = false,
    val focus: List<FocusedAssignment> = emptyList()
)
