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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.vdvoem.core.domain.model.Lesson
import app.vdvoem.core.ui.components.EmptyState
import app.vdvoem.core.ui.components.InfoPill
import app.vdvoem.core.ui.components.VdvoemCard
import app.vdvoem.core.ui.theme.VdvoemSpacing
import app.vdvoem.core.ui.theme.VdvoemTheme
import java.time.LocalDate

/**
 * Расписание уроков пары: горизонтальная лента дат сверху, под ней — уроки выбранного дня.
 *
 * Добавление и правка — одно диалоговое окно; всё сохраняется в Room и уезжает в синк.
 * В уроке можно отметить, кто берёт учебник на этот предмет.
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
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val dates = remember(today) {
        val start = weekStart(today)
        List(TIMETABLE_STRIP_DAYS) { start.plusDays(it.toLong()) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            DayStrip(
                dates = dates,
                selected = state.selectedDate,
                today = today,
                onSelect = onSelectDate
            )
            Text(
                text = dayTitle(state.selectedDate),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(
                    start = VdvoemSpacing.screenHorizontal,
                    end = VdvoemSpacing.screenHorizontal,
                    top = VdvoemSpacing.sm,
                    bottom = VdvoemSpacing.sm
                )
            )
            when {
                state.isEmpty -> LazyColumn(
                    contentPadding = PaddingValues(
                        start = VdvoemSpacing.screenHorizontal,
                        end = VdvoemSpacing.screenHorizontal,
                        bottom = VdvoemSpacing.listBottom
                    )
                ) {
                    item {
                        EmptyState(
                            icon = Icons.Filled.DateRange,
                            title = stringResource(R.string.timetable_empty_title),
                            message = stringResource(R.string.timetable_empty_message),
                            actionLabel = stringResource(R.string.timetable_add),
                            onAction = onAdd,
                            modifier = Modifier.padding(top = VdvoemSpacing.lg)
                        )
                    }
                }

                state.dayLessons.isEmpty() -> LazyColumn(
                    contentPadding = PaddingValues(
                        start = VdvoemSpacing.screenHorizontal,
                        end = VdvoemSpacing.screenHorizontal,
                        bottom = VdvoemSpacing.listBottom
                    )
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.timetable_day_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = VdvoemSpacing.xxl)
                        )
                    }
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(
                        start = VdvoemSpacing.screenHorizontal,
                        end = VdvoemSpacing.screenHorizontal,
                        bottom = VdvoemSpacing.listBottom
                    ),
                    verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
                ) {
                    items(state.dayLessons, key = { it.id }) { lesson ->
                        LessonCard(lesson = lesson, onClick = { onEdit(lesson) })
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

/** Горизонтальная лента дат: сегодняшняя подписана, выбранная залита цветом. */
@Composable
private fun DayStrip(
    dates: List<LocalDate>,
    selected: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit
) {
    val listState = rememberLazyListState()
    // Если выбранный день ушёл за край — подкручиваем ленту, чтобы он был виден.
    LaunchedEffect(selected) {
        val index = dates.indexOf(selected)
        if (index >= 0 && listState.layoutInfo.visibleItemsInfo.none { it.index == index }) {
            listState.animateScrollToItem(index)
        }
    }
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = VdvoemSpacing.screenHorizontal,
            end = VdvoemSpacing.screenHorizontal,
            top = VdvoemSpacing.sm
        ),
        horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
    ) {
        items(dates, key = { it.toEpochDay() }) { date ->
            DayChip(
                date = date,
                selected = date == selected,
                today = date == today,
                onClick = { onSelect(date) }
            )
        }
    }
}

@Composable
private fun DayChip(
    date: LocalDate,
    selected: Boolean,
    today: Boolean,
    onClick: () -> Unit
) {
    val container = when {
        selected -> MaterialTheme.colorScheme.primary
        today -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val content = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        today -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = content,
        modifier = Modifier.widthIn(min = VdvoemSpacing.huge + VdvoemSpacing.lg)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = VdvoemSpacing.md,
                vertical = VdvoemSpacing.sm
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = weekdayShort(date),
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (today) stringResource(R.string.timetable_today) else " ",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun LessonCard(lesson: Lesson, onClick: () -> Unit) {
    VdvoemCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                if (lesson.textbookBy.isNotBlank()) {
                    Spacer(Modifier.height(VdvoemSpacing.sm))
                    InfoPill(
                        text = stringResource(
                            R.string.timetable_textbook_taken,
                            lesson.textbookBy
                        )
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
                OutlinedTextField(
                    value = draft.textbookBy,
                    onValueChange = { value -> onUpdate { it.copy(textbookBy = value) } },
                    label = { Text(stringResource(R.string.timetable_textbook)) },
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
private fun weekdayShort(date: LocalDate): String {
    val weekdays = stringArrayResource(R.array.timetable_weekdays)
    return weekdays.getOrElse(date.dayOfWeek.value - 1) { weekdays.first() }
}

/** «Понедельник, 23 ноября». */
@Composable
private fun dayTitle(date: LocalDate): String {
    val weekdays = stringArrayResource(R.array.timetable_weekdays_full)
    val months = stringArrayResource(R.array.timetable_months)
    val weekday = weekdays.getOrElse(date.dayOfWeek.value - 1) { weekdays.first() }
    val month = months.getOrElse(date.monthValue - 1) { months.first() }
    return "$weekday, ${date.dayOfMonth} $month"
}

@Preview(showBackground = true)
@Composable
private fun TimetablePreview() {
    VdvoemTheme {
        TimetableScreen(
            state = TimetableUiState(
                lessons = listOf(
                    Lesson(
                        id = "1", subject = "Математика", weekday = 1,
                        startMinute = 540, endMinute = 585, room = "204",
                        textbookBy = "Варя"
                    ),
                    Lesson(id = "2", subject = "Литература", weekday = 1, startMinute = 600, endMinute = 645),
                    Lesson(
                        id = "3", subject = "Физика", weekday = 3,
                        startMinute = 510, endMinute = 555, teacher = "И. П. Петров"
                    )
                )
            ),
            onAdd = {},
            onEdit = {},
            onDismiss = {},
            onUpdate = {},
            onSave = {},
            onDelete = {},
            onSelectDate = {}
        )
    }
}