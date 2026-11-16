package app.vdvoem.feature.timetable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.vdvoem.core.domain.model.Lesson
import app.vdvoem.core.ui.components.EmptyState
import app.vdvoem.core.ui.components.VdvoemCard
import app.vdvoem.core.ui.theme.VdvoemSizes
import app.vdvoem.core.ui.theme.VdvoemSpacing
import app.vdvoem.core.ui.theme.VdvoemTheme

/**
 * Расписание уроков пары: неделя списком, уроки по дням.
 *
 * Добавление и правка — одно диалоговое окно; всё сохраняется в Room и уезжает в синк.
 */
@Composable
fun TimetableScreen(
    state: TimetableUiState,
    onAdd: () -> Unit,
    onEdit: (Lesson) -> Unit,
    onDismiss: () -> Unit,
    onUpdate: ((LessonDraft) -> LessonDraft) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (state.isEmpty) {
                EmptyState(
                    icon = Icons.Filled.DateRange,
                    title = stringResource(R.string.timetable_empty_title),
                    message = stringResource(R.string.timetable_empty_message),
                    actionLabel = stringResource(R.string.timetable_add),
                    onAction = onAdd,
                    modifier = Modifier.padding(top = VdvoemSpacing.huge)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = VdvoemSpacing.screenHorizontal,
                        end = VdvoemSpacing.screenHorizontal,
                        top = VdvoemSpacing.xs,
                        bottom = VdvoemSpacing.listBottom
                    ),
                    verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
                ) {
                    state.byDay.forEach { (day, lessons) ->
                        item(key = "day-$day") {
                            Text(
                                text = weekdayName(day),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = VdvoemSpacing.md)
                            )
                        }
                        items(lessons, key = { it.id }) { lesson ->
                            LessonCard(lesson = lesson, onClick = { onEdit(lesson) })
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onAdd,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.timetable_add)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(VdvoemSpacing.screenHorizontal)
                .padding(bottom = VdvoemSpacing.lg)
        )
    }

    state.draft?.let { draft ->
        LessonDialog(
            draft = draft,
            onDismiss = onDismiss,
            onUpdate = onUpdate,
            onSave = onSave,
            onDelete = onDelete
        )
    }
}

@Composable
private fun LessonCard(lesson: Lesson, onClick: () -> Unit) {
    VdvoemCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(VdvoemSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lesson.subject,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = lesson.timeRange,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val details = listOf(lesson.room, lesson.teacher)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                if (details.isNotBlank()) {
                    Spacer(Modifier.height(VdvoemSpacing.xxs))
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LessonDialog(
    draft: LessonDraft,
    onDismiss: () -> Unit,
    onUpdate: ((LessonDraft) -> LessonDraft) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    val weekdays = stringArrayResource(R.array.timetable_weekdays)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (draft.id == null) R.string.timetable_new else R.string.timetable_edit
                )
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
                    value = draft.subject,
                    onValueChange = { value -> onUpdate { it.copy(subject = value, error = null) } },
                    label = { Text(stringResource(R.string.timetable_subject)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.timetable_day),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.xs)
                ) {
                    items(weekdays.size) { index ->
                        val day = index + 1
                        FilterChip(
                            selected = draft.weekday == day,
                            onClick = { onUpdate { it.copy(weekday = day) } },
                            label = { Text(weekdays[index]) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)) {
                    OutlinedTextField(
                        value = draft.start,
                        onValueChange = { value -> onUpdate { it.copy(start = value, error = null) } },
                        label = { Text(stringResource(R.string.timetable_start)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = draft.end,
                        onValueChange = { value -> onUpdate { it.copy(end = value, error = null) } },
                        label = { Text(stringResource(R.string.timetable_end)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = draft.room,
                    onValueChange = { value -> onUpdate { it.copy(room = value) } },
                    label = { Text(stringResource(R.string.timetable_room)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.teacher,
                    onValueChange = { value -> onUpdate { it.copy(teacher = value) } },
                    label = { Text(stringResource(R.string.timetable_teacher)) },
                    singleLine = true,
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
                    TextButton(onClick = onDelete) {
                        Text(
                            text = stringResource(R.string.timetable_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(stringResource(R.string.timetable_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.timetable_cancel))
            }
        }
    )
}

@Composable
private fun weekdayName(day: Int): String {
    val weekdays = stringArrayResource(R.array.timetable_weekdays)
    return weekdays.getOrElse(day - 1) { weekdays.first() }
}

@Preview(showBackground = true)
@Composable
private fun TimetablePreview() {
    VdvoemTheme {
        TimetableScreen(
            state = TimetableUiState(
                lessons = listOf(
                    Lesson(id = "1", subject = "Математика", weekday = 1, startMinute = 540, endMinute = 585, room = "204"),
                    Lesson(id = "2", subject = "Литература", weekday = 1, startMinute = 600, endMinute = 645),
                    Lesson(id = "3", subject = "Физика", weekday = 3, startMinute = 510, endMinute = 555, teacher = "И. П. Петров")
                )
            ),
            onAdd = {},
            onEdit = {},
            onDismiss = {},
            onUpdate = {},
            onSave = {},
            onDelete = {}
        )
    }
}