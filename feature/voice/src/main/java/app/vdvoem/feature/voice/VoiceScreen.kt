package app.vdvoem.feature.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import app.vdvoem.core.ui.components.EmptyState
import app.vdvoem.core.ui.components.InfoPill
import app.vdvoem.core.ui.components.VdvoemCard
import app.vdvoem.core.ui.theme.VdvoemSizes
import app.vdvoem.core.ui.theme.VdvoemSpacing
import app.vdvoem.core.ui.theme.VdvoemTheme
import app.vdvoem.feature.voice.data.VoiceNote
import java.util.Locale

/**
 * Экран голосовых заметок: список записей, проигрывание, запись и удаление.
 *
 * Разрешение `RECORD_AUDIO` запрашивается здесь: запись стартует только после
 * подтверждения, отказ показывается диалогом с объяснением.
 */
@Composable
fun VoiceScreen(
    state: VoiceUiState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onTogglePlay: (VoiceNote) -> Unit,
    onDeleteRequest: (VoiceNote) -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteCancel: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onStartRecording()
        } else {
            permissionDenied = true
        }
    }

    val startOrStop: () -> Unit = {
        if (state.isRecording) {
            onStopRecording()
        } else {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) onStartRecording() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (state.isEmpty) {
            EmptyState(
                icon = Icons.Filled.PlayArrow,
                title = stringResource(R.string.voice_empty_title),
                message = stringResource(R.string.voice_empty_message),
                actionLabel = stringResource(R.string.voice_record),
                onAction = startOrStop,
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
                itemsIndexed(state.notes, key = { _, note -> note.id }) { index, note ->
                    VoiceNoteCard(
                        note = note,
                        index = index,
                        isPlaying = state.playingId == note.id,
                        onToggle = { onTogglePlay(note) },
                        onDelete = { onDeleteRequest(note) }
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = startOrStop,
            icon = {
                Icon(
                    imageVector = if (state.isRecording) Icons.Filled.Close else Icons.Filled.PlayArrow,
                    contentDescription = null
                )
            },
            text = {
                Text(
                    if (state.isRecording) {
                        stringResource(
                            R.string.voice_stop_with_time,
                            formatDuration(state.elapsedSeconds)
                        )
                    } else {
                        stringResource(R.string.voice_record)
                    }
                )
            },
            containerColor = if (state.isRecording) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            contentColor = if (state.isRecording) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(VdvoemSpacing.screenHorizontal)
                .padding(bottom = VdvoemSpacing.lg)
        )
    }

    state.pendingDelete?.let {
        AlertDialog(
            onDismissRequest = onDeleteCancel,
            title = { Text(stringResource(R.string.voice_delete_confirm_title)) },
            text = { Text(stringResource(R.string.voice_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = onDeleteConfirm) {
                    Text(
                        text = stringResource(R.string.voice_delete_confirm_action),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDeleteCancel) {
                    Text(stringResource(R.string.voice_cancel))
                }
            }
        )
    }

    if (permissionDenied) {
        AlertDialog(
            onDismissRequest = { permissionDenied = false },
            title = { Text(stringResource(R.string.voice_permission_title)) },
            text = { Text(stringResource(R.string.voice_permission_message)) },
            confirmButton = {
                TextButton(onClick = { permissionDenied = false }) {
                    Text(stringResource(R.string.voice_ok))
                }
            }
        )
    }

    state.errorRes?.let { message ->
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text(stringResource(R.string.voice_error_title)) },
            text = { Text(stringResource(message)) },
            confirmButton = {
                TextButton(onClick = onDismissError) {
                    Text(stringResource(R.string.voice_ok))
                }
            }
        )
    }
}

@Composable
private fun VoiceNoteCard(
    note: VoiceNote,
    index: Int,
    isPlaying: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    VdvoemCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Close else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(
                        if (isPlaying) R.string.voice_stop else R.string.voice_play
                    )
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = VdvoemSpacing.md)
            ) {
                Text(
                    text = stringResource(R.string.voice_note_title, index + 1),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatNoteTime(note.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            InfoPill(text = formatDuration(note.durationSeconds))
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.voice_delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(VdvoemSizes.iconMd)
                )
            }
        }
    }
}

/** `m:ss` — для длительности заметки и таймера записи. */
private fun formatDuration(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return String.format(Locale.getDefault(), "%d:%02d", safe / 60, safe % 60)
}

private fun formatNoteTime(millis: Long): String =
    java.time.Instant.ofEpochMilli(millis)
        .atZone(java.time.ZoneId.systemDefault())
        .format(NOTE_DATE_FORMAT)

private val NOTE_DATE_FORMAT: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.getDefault())

@Preview(showBackground = true)
@Composable
private fun VoiceScreenPreview() {
    VdvoemTheme {
        VoiceScreen(
            state = VoiceUiState(
                notes = listOf(
                    VoiceNote(
                        id = "1",
                        title = "",
                        filePath = "",
                        durationMs = 42_000L,
                        createdAt = 0L
                    ),
                    VoiceNote(
                        id = "2",
                        title = "",
                        filePath = "",
                        durationMs = 7_500L,
                        createdAt = 0L
                    )
                )
            ),
            onStartRecording = {},
            onStopRecording = {},
            onTogglePlay = {},
            onDeleteRequest = {},
            onDeleteConfirm = {},
            onDeleteCancel = {},
            onDismissError = {}
        )
    }
}