package com.zensa.app.ui.screens.assignments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zensa.app.core.network.ApiResult
import com.zensa.app.data.entity.AssignmentEntity
import com.zensa.app.data.entity.ModuleEntity
import com.zensa.app.data.preferences.TokenStorage
import com.zensa.app.data.repository.AssignmentRepository
import com.zensa.app.data.repository.ModuleRepository
import com.zensa.app.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * AssignmentsViewModel
 *
 * Owns the Assignments list screen state.
 *
 * Reads: a reactive Flow of assignments + modules from Room.
 * Writes: delegated to AssignmentRepository.
 *
 * Filtering and searching happen entirely in memory so the UI
 * remains instant even on slow devices.
 */
class AssignmentsViewModel(
    private val assignmentRepository: AssignmentRepository =
        ServiceLocator.assignmentRepository,
    private val moduleRepository: ModuleRepository =
        ServiceLocator.moduleRepository,
    private val tokenStorage: TokenStorage =
        ServiceLocator.tokenStorage()
) : ViewModel() {

    private val userId: String? get() = tokenStorage.getUserId()

    // ---------- Filter state (driven by the UI) ----------

    private val _filterState = MutableStateFlow(AssignmentFilterState())
    val filterState: StateFlow<AssignmentFilterState> = _filterState.asStateFlow()

    // ---------- Assignments (Room-driven, reactive) ----------

    val assignments: StateFlow<List<AssignmentEntity>> =
        assignmentRepository.observeAssignments(userId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // ---------- Modules (for the "filter by module" dropdown) ----------

    val modules: StateFlow<List<ModuleEntity>> =
        moduleRepository.observeModules(userId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // ---------- Assignments + filter applied ----------

    val visibleAssignments: StateFlow<List<AssignmentEntity>> =
        combine(assignments, _filterState) { all, filter ->
            applyFilter(all, filter)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // ---------- Actions ----------

    /** Pull the latest list from the backend. Errors are silent. */
    fun refresh() {
        viewModelScope.launch {
            assignmentRepository.refresh()
        }
    }

    fun onSearchQueryChange(value: String) {
        _filterState.update { it.copy(searchQuery = value) }
    }

    fun onModuleFilterChange(moduleLocalId: String?) {
        _filterState.update { it.copy(moduleLocalId = moduleLocalId) }
    }

    fun onPriorityFilterChange(priority: String?) {
        _filterState.update { it.copy(priority = priority) }
    }

    fun onStatusFilterChange(status: String?) {
        _filterState.update { it.copy(status = status) }
    }

    fun clearFilters() {
        _filterState.value = AssignmentFilterState()
    }

    fun toggleCompleted(localId: String, completed: Boolean) {
        viewModelScope.launch {
            assignmentRepository.setCompleted(localId, completed)
        }
    }

    fun delete(localId: String) {
        viewModelScope.launch {
            assignmentRepository.delete(localId)
        }
    }

    // ---------- Internals ----------

    private fun applyFilter(
        all: List<AssignmentEntity>,
        filter: AssignmentFilterState
    ): List<AssignmentEntity> {
        return all.filter { a ->
            val matchesQuery = filter.searchQuery.isBlank() ||
                a.title.contains(filter.searchQuery, ignoreCase = true) ||
                a.description.contains(filter.searchQuery, ignoreCase = true)

            val matchesModule = filter.moduleLocalId == null ||
                a.moduleLocalId == filter.moduleLocalId

            val matchesPriority = filter.priority == null ||
                a.priority.equals(filter.priority, ignoreCase = true)

            val matchesStatus = when (filter.status) {
                null -> true
                "PENDING" -> !a.isCompleted
                "COMPLETED" -> a.isCompleted
                else -> true
            }

            matchesQuery && matchesModule && matchesPriority && matchesStatus
        }
    }
}

/**
 * Immutable filter state for the Assignments screen.
 */
data class AssignmentFilterState(
    val searchQuery: String = "",
    val moduleLocalId: String? = null,
    val priority: String? = null,
    val status: String? = null
)
