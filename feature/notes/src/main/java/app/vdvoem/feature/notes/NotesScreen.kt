package app.vdvoem.feature.notes

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import app.vdvoem.core.ui.components.EmptyState
import app.vdvoem.core.ui.components.VdvoemCard
import app.vdvoem.core.ui.theme.VdvoemSpacing
import app.vdvoem.core.ui.theme.VdvoemTheme
import app.vdvoem.feature.notes.data.Note
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Личные заметки: список, редактор и подтверждение удаления.
 *
 * Всё хранится только на этом устройстве — партнёр заметок не видит.
 */
@Composable
fun NotesScreen(
    state: NotesUiState,
    onAdd: () -> Unit,
    onEdit: (Note) -> Unit,
    onDismiss: () -> Unit,
    onUpdate: ((NoteDraft) -> NoteDraft) -> Unit,
    onSave: () -> Unit,
    onDeleteRequest: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (state.isEmpty) {
            EmptyState(
                icon = Icons.Filled.Edit,
                title = stringResource(R.string.notes_empty_title),
                message = stringResource(R.string.notes_empty_message),
                actionLabel = stringResource(R.string.notes_add),
                onAction = onAdd,
                modifier = Modifier.padding(top = VdvoemSpacing.huge)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = VdvoemSpacing.screenHorizontal,
                    end = VdvoemSpacing.screenHorizontal,
                    top = VdvoemSpacing.sm,
                    bottom = VdvoemSpacing.listBottom
                ),
                verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
            ) {
                items(state.notes, key = { it.id }) { note ->
                    NoteCard(note = note, onClick = { onEdit(note) })
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onAdd,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.notes_add)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(VdvoemSpacing.screenHorizontal)
                .padding(bottom = VdvoemSpacing.lg)
        )
    }

    state.draft?.let { draft ->
        NoteEditorDialog(
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
            title = { Text(stringResource(R.string.notes_delete_confirm_title)) },
            text = { Text(stringResource(R.string.notes_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = onDeleteConfirm) {
                    Text(
                        text = stringResource(R.string.notes_delete_confirm_action),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDeleteCancel) {
                    Text(stringResource(R.string.notes_cancel))
                }
            }
        )
    }
}

@Composable
private fun NoteCard(note: Note, onClick: () -> Unit) {
    VdvoemCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = note.title.ifBlank { stringResource(R.string.notes_untitled) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = formatNoteDate(note.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = VdvoemSpacing.sm)
            )
        }
        if (note.preview.isNotBlank()) {
            Text(
                text = note.preview,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = VdvoemSpacing.xs)
            )
        }
    }
}

@Composable
private fun NoteEditorDialog(
    draft: NoteDraft,
    onDismiss: () -> Unit,
    onUpdate: ((NoteDraft) -> NoteDraft) -> Unit,
    onSave: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(if (draft.id == null) R.string.notes_new else R.string.notes_edit)
            )
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
                    label = { Text(stringResource(R.string.notes_field_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.body,
                    onValueChange = { value -> onUpdate { it.copy(body = value, error = null) } },
                    label = { Text(stringResource(R.string.notes_field_body)) },
                    minLines = 4,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
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
                            text = stringResource(R.string.notes_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(stringResource(R.string.notes_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.notes_cancel))
            }
        }
    )
}

/** Дата в локальной зоне: «12 ноя, 18:40». */
private fun formatNoteDate(millis: Long): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(DATE_FORMAT)

private val DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.getDefault())

@Preview(showBackground = true)
@Composable
private fun NotesPreview() {
    VdvoemTheme {
        NotesScreen(
            state = NotesUiState(
                notes = listOf(
                    Note(
                        id = "1",
                        title = "Идеи для подарка",
                        body = "Тёплый свитер\nКнига про путешествия\nЧто-нибудь для кухни",
                        createdAt = 0L,
                        updatedAt = 0L
                    ),
                    Note(
                        id = "2",
                        title = "",
                        body = "Купить молоко и хлеб по дороге домой",
                        createdAt = 0L,
                        updatedAt = 0L
                    )
                )
            ),
            onAdd = {},
            onEdit = {},
            onDismiss = {},
            onUpdate = {},
            onSave = {},
            onDeleteRequest = {},
            onDeleteConfirm = {},
            onDeleteCancel = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotesEmptyPreview() {
    VdvoemTheme {
        NotesScreen(
            state = NotesUiState(),
            onAdd = {},
            onEdit = {},
            onDismiss = {},
            onUpdate = {},
            onSave = {},
            onDeleteRequest = {},
            onDeleteConfirm = {},
            onDeleteCancel = {}
        )
    }
}