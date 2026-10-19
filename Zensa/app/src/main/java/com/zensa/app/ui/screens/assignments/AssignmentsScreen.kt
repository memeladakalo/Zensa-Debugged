package com.zensa.app.ui.screens.assignments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zensa.app.data.entity.AssignmentEntity
import com.zensa.app.ui.theme.PriorityHigh
import com.zensa.app.ui.theme.PriorityLow
import com.zensa.app.ui.theme.PriorityMedium
import com.zensa.app.ui.theme.PriorityUrgent
import com.zensa.app.ui.theme.ZensaTheme

/**
 * AssignmentsScreen
 *
 * Lists all assignments for the current user, with:
 *   - Search bar (title + description)
 *   - Priority filter chips
 *   - Status filter chips (All / Pending / Completed)
 *   - FAB to create a new assignment
 *   - Tap a card to open Assignment Details
 *   - Checkbox to toggle completed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentsScreen(
    onBack: () -> Unit,
    onAddAssignment: () -> Unit,
    onOpenAssignment: (String) -> Unit,
    viewModel: AssignmentsViewModel = viewModel()
) {
    val visible by viewModel.visibleAssignments.collectAsStateWithLifecycle()
    val filter by viewModel.filterState.collectAsStateWithLifecycle()

    // Refresh from the server when the screen is first shown.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assignments") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddAssignment,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New assignment")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {

            // ---------- Search ----------
            OutlinedTextField(
                value = filter.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (filter.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
                placeholder = { Text("Search assignments…") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ---------- Status filter ----------
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = filter.status == null,
                    onClick = { viewModel.onStatusFilterChange(null) },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = filter.status == "PENDING",
                    onClick = { viewModel.onStatusFilterChange("PENDING") },
                    label = { Text("Pending") }
                )
                FilterChip(
                    selected = filter.status == "COMPLETED",
                    onClick = { viewModel.onStatusFilterChange("COMPLETED") },
                    label = { Text("Completed") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ---------- Priority filter ----------
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = filter.priority == null,
                    onClick = { viewModel.onPriorityFilterChange(null) },
                    label = { Text("Any priority") }
                )
                PriorityFilterChip(
                    label = "Low",
                    value = "LOW",
                    selected = filter.priority == "LOW",
                    onClick = viewModel::onPriorityFilterChange
                )
                PriorityFilterChip(
                    label = "High",
                    value = "HIGH",
                    selected = filter.priority == "HIGH",
                    onClick = viewModel::onPriorityFilterChange
                )
                PriorityFilterChip(
                    label = "Urgent",
                    value = "URGENT",
                    selected = filter.priority == "URGENT",
                    onClick = viewModel::onPriorityFilterChange
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ---------- List ----------
            if (visible.isEmpty()) {
                EmptyAssignments()
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(visible, key = { it.localId }) { assignment ->
                        AssignmentCard(
                            assignment = assignment,
                            onToggle = { completed ->
                                viewModel.toggleCompleted(assignment.localId, completed)
                            },
                            onClick = { onOpenAssignment(assignment.localId) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun PriorityFilterChip(
    label: String,
    value: String,
    selected: Boolean,
    onClick: (String?) -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = { onClick(if (selected) null else value) },
        label = { Text(label) }
    )
}

@Composable
private fun AssignmentCard(
    assignment: AssignmentEntity,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Checkbox(
                checked = assignment.isCompleted,
                onCheckedChange = { checked -> onToggle(checked) }
            )

            Spacer(modifier = Modifier.size(8.dp))

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(priorityColour(assignment.priority))
            )

            Spacer(modifier = Modifier.size(10.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = assignment.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (assignment.isCompleted)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Due ${formatDue(assignment.dueDate)} · ${assignment.priority}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyAssignments() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No assignments yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tap the + button to create your first one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun priorityColour(priority: String): Color = when (priority.uppercase()) {
    "LOW" -> PriorityLow
    "MEDIUM" -> PriorityMedium
    "HIGH" -> PriorityHigh
    "URGENT" -> PriorityUrgent
    else -> PriorityMedium
}

/**
 * Renders an ISO date-time as a short human-readable string.
 * Kept simple for now — proper date formatting comes with Settings.
 */
private fun formatDue(iso: String): String {
    return try {
        iso.substringBefore("T")
    } catch (_: Exception) {
        iso
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AssignmentsScreenPreview() {
    ZensaTheme {
        AssignmentsScreen(
            onBack = {},
            onAddAssignment = {},
            onOpenAssignment = {}
        )
    }
}
