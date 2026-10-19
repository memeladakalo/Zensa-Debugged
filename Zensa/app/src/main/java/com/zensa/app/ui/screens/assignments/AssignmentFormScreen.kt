package com.zensa.app.ui.screens.assignments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zensa.app.ui.theme.ZensaError
import com.zensa.app.ui.theme.ZensaTheme

/**
 * AssignmentFormScreen — used for BOTH create and edit.
 *
 * The mode is decided by whether [assignmentLocalId] is null:
 *   null     → CREATE
 *   non-null → EDIT (loads existing assignment from Room)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentFormScreen(
    assignmentLocalId: String?,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AssignmentFormViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Load the assignment if we're editing.
    androidx.compose.runtime.LaunchedEffect(assignmentLocalId) {
        viewModel.start(assignmentLocalId)
    }

    val isEdit = assignmentLocalId != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit assignment" else "New assignment") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // ---------- Title ----------
                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::onTitleChange,
                    label = { Text("Title") },
                    singleLine = true,
                    isError = state.titleError != null,
                    supportingText = {
                        if (state.titleError != null)
                            Text(state.titleError!!, color = ZensaError)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ---------- Description ----------
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = { Text("Description (optional)") },
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ---------- Due date ----------
                Text(
                    text = "Due date",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = state.dueDate,
                    onValueChange = viewModel::onDueDateChange,
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    isError = state.dueDateError != null,
                    supportingText = {
                        if (state.dueDateError != null)
                            Text(state.dueDateError!!, color = ZensaError)
                        else
                            Text(
                                "Format: 2026-04-15",
                                style = MaterialTheme.typography.labelMedium
                            )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ---------- Priority ----------
                Text(
                    text = "Priority",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("LOW", "MEDIUM", "HIGH", "URGENT").forEach { p ->
                        FilterChip(
                            selected = state.priority == p,
                            onClick = { viewModel.onPriorityChange(p) },
                            label = { Text(p) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ---------- Module selection ----------
                Text(
                    text = "Module (optional)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (state.availableModules.isEmpty()) {
                    Text(
                        text = "No modules yet — you can assign this later.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = state.moduleLocalId == null,
                            onClick = { viewModel.onModuleChange(null) },
                            label = { Text("None") }
                        )
                        state.availableModules.take(3).forEach { module ->
                            FilterChip(
                                selected = state.moduleLocalId == module.localId,
                                onClick = { viewModel.onModuleChange(module.localId) },
                                label = { Text(module.moduleCode) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ---------- General error ----------
                if (state.generalError != null) {
                    Text(
                        text = state.generalError!!,
                        color = ZensaError,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ---------- Save button ----------
                Button(
                    onClick = { viewModel.save(onSaved) },
                    enabled = !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isEdit) "Save changes" else "Create assignment",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AssignmentFormScreenPreview() {
    ZensaTheme {
        AssignmentFormScreen(
            assignmentLocalId = null,
            onNavigateBack = {},
            onSaved = {}
        )
    }
}
