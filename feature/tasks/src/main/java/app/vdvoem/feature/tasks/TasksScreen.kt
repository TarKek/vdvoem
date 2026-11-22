package app.vdvoem.feature.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import app.vdvoem.core.domain.model.SharedTask
import app.vdvoem.core.domain.time.Dates
import app.vdvoem.core.ui.components.EmptyState
import app.vdvoem.core.ui.components.VdvoemCard
import app.vdvoem.core.ui.theme.VdvoemSpacing
import app.vdvoem.core.ui.theme.VdvoemTheme
import java.time.LocalDate

/** Общий список дел пары с отметкой выполнения, сроком и подтверждением удаления. */
@Composable
fun TasksScreen(
    state: TasksUiState,
    onAdd: () -> Unit,
    onEdit: (SharedTask) -> Unit,
    onToggle: (SharedTask) -> Unit,
    onDismiss: () -> Unit,
    onUpdate: ((TaskDraft) -> TaskDraft) -> Unit,
    onSave: () -> Unit,
    onDeleteRequest: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteCancel: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )

            state.isEmpty -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Filled.CheckCircle,
                    title = stringResource(R.string.tasks_empty_title),
                    message = stringResource(R.string.tasks_empty_message),
                    modifier = Modifier.padding(horizontal = VdvoemSpacing.xl)
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = VdvoemSpacing.screenHorizontal,
                    end = VdvoemSpacing.screenHorizontal,
                    top = VdvoemSpacing.sm,
                    bottom = VdvoemSpacing.listBottom
                ),
                verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
            ) {
                items(state.tasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onToggle = { onToggle(task) },
                        onClick = { onEdit(task) }
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onAdd,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.tasks_add)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(VdvoemSpacing.screenHorizontal)
                .padding(bottom = VdvoemSpacing.lg)
        )
    }

    state.draft?.let { draft ->
        TaskEditorDialog(
            draft = draft,
            onDismiss = onDismiss,
            onUpdate = onUpdate,
            onSave = onSave,
            onDeleteRequest = onDeleteRequest
        )
    }

    state.pendingDelete?.let {
        AlertDialog(
            onDismissRequest = onDeleteCancel,
            title = { Text(stringResource(R.string.tasks_delete_confirm_title)) },
            text = { Text(stringResource(R.string.tasks_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = onDeleteConfirm) {
                    Text(
                        text = stringResource(R.string.tasks_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDeleteCancel) {
                    Text(stringResource(R.string.tasks_cancel))
                }
            }
        )
    }

    state.error?.let { message ->
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text(stringResource(R.string.tasks_error_dialog_title)) },
            text = { Text(stringResource(message)) },
            confirmButton = {
                TextButton(onClick = onDismissError) {
                    Text(stringResource(R.string.tasks_error_dismiss))
                }
            }
        )
    }
}

@Composable
private fun TaskCard(
    task: SharedTask,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    val completedLabel = stringResource(
        if (task.isCompleted) R.string.tasks_mark_active else R.string.tasks_mark_completed
    )
    VdvoemCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                modifier = Modifier
                    .padding(end = VdvoemSpacing.xs)
                    .semantics { contentDescription = completedLabel }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.note.isNotBlank()) {
                    Text(
                        text = task.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = VdvoemSpacing.xs)
                    )
                }
                task.dueAt?.let { dueAt ->
                    val dueDate = Dates.toLocalDate(dueAt)
                    val overdue = !task.isCompleted && dueDate.isBefore(LocalDate.now())
                    Text(
                        text = stringResource(R.string.tasks_field_due, Dates.format(dueDate)),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (overdue) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.padding(top = VdvoemSpacing.sm)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditorDialog(
    draft: TaskDraft,
    onDismiss: () -> Unit,
    onUpdate: ((TaskDraft) -> TaskDraft) -> Unit,
    onSave: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (draft.id == null) R.string.tasks_new else R.string.tasks_edit))
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
            ) {
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = { value -> onUpdate { it.copy(title = value, error = null) } },
                    label = { Text(stringResource(R.string.tasks_field_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.note,
                    onValueChange = { value -> onUpdate { it.copy(note = value, error = null) } },
                    label = { Text(stringResource(R.string.tasks_field_note)) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.DateRange, contentDescription = null)
                        Text(
                            text = draft.dueAt?.let {
                                stringResource(
                                    R.string.tasks_field_due,
                                    Dates.format(Dates.toLocalDate(it))
                                )
                            } ?: stringResource(R.string.tasks_field_no_due),
                            modifier = Modifier.padding(start = VdvoemSpacing.xs)
                        )
                    }
                    if (draft.dueAt != null) {
                        TextButton(onClick = { onUpdate { it.copy(dueAt = null) } }) {
                            Text(stringResource(R.string.tasks_clear_due))
                        }
                    }
                }
                draft.error?.let { message ->
                    Text(
                        text = stringResource(message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (draft.id != null) {
                    TextButton(onClick = onDeleteRequest) {
                        Text(
                            text = stringResource(R.string.tasks_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(stringResource(R.string.tasks_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.tasks_cancel))
            }
        }
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = draft.dueAt?.let(Dates::toPickerMillis)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onUpdate { it.copy(dueAt = Dates.fromPickerMillis(millis)) }
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.tasks_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.tasks_cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TasksPreview() {
    VdvoemTheme {
        TasksScreen(
            state = TasksUiState(
                isLoading = false,
                tasks = listOf(
                    SharedTask(
                        id = "1",
                        pairId = "pair",
                        title = "Купить продукты на ужин",
                        note = "Не забыть любимый сыр",
                        dueAt = Dates.startOfDay(LocalDate.of(2026, 11, 25)),
                        createdAt = 0L,
                        updatedAt = 0L
                    ),
                    SharedTask(
                        id = "2",
                        pairId = "pair",
                        title = "Забронировать столик",
                        isCompleted = true,
                        createdAt = 0L,
                        updatedAt = 0L,
                        version = 2L
                    )
                )
            ),
            onAdd = {},
            onEdit = {},
            onToggle = {},
            onDismiss = {},
            onUpdate = {},
            onSave = {},
            onDeleteRequest = {},
            onDeleteConfirm = {},
            onDeleteCancel = {},
            onDismissError = {}
        )
    }
}