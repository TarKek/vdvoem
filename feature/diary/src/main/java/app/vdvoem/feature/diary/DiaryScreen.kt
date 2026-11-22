@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package app.vdvoem.feature.diary

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.vdvoem.core.domain.model.DiaryEntry
import app.vdvoem.core.domain.model.DiaryMood
import app.vdvoem.core.domain.model.DiaryReminder
import app.vdvoem.core.domain.time.Dates
import app.vdvoem.core.ui.components.EmptyState
import app.vdvoem.core.ui.components.InfoPill
import app.vdvoem.core.ui.components.SectionHeader
import app.vdvoem.core.ui.components.VdvoemCard
import app.vdvoem.core.ui.theme.VdvoemSizes
import app.vdvoem.core.ui.theme.VdvoemSpacing
import app.vdvoem.core.ui.theme.VdvoemTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Общий дневник пары: лента записей, редактор в нижней шторке и мягкое удаление.
 *
 * Всё состояние редактора переживает поворот: id, дата, настроение, заголовок и текст
 * лежат в [rememberSaveable].
 */
@Composable
fun DiaryScreen(
    state: DiaryUiState,
    onSave: (id: String?, dateMillis: Long, mood: DiaryMood, title: String, body: String) -> Unit,
    onDelete: (id: String) -> Unit,
    onSaveReminder: (id: String?, text: String, remindAt: Long, isPrivate: Boolean) -> Unit,
    onDeleteReminder: (id: String) -> Unit,
    onReminderDone: (id: String, done: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var editorDateMillis by rememberSaveable {
        mutableLongStateOf(Dates.startOfDay(Dates.today()))
    }
    var editorMoodName by rememberSaveable { mutableStateOf(DiaryMood.CALM.name) }
    var editorTitle by rememberSaveable { mutableStateOf("") }
    var editorBody by rememberSaveable { mutableStateOf("") }
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }

    // Редактор напоминания: дата — UTC-полночь (как в DatePicker), время — минуты от полуночи,
    // чтобы оба значения пережили поворот без Parcelable-обёрток.
    val initialReminderSlot = remember { defaultReminderSlot() }
    var reminderEditorOpen by rememberSaveable { mutableStateOf(false) }
    var editingReminderId by rememberSaveable { mutableStateOf<String?>(null) }
    var reminderText by rememberSaveable { mutableStateOf("") }
    var reminderDateMillis by rememberSaveable { mutableLongStateOf(initialReminderSlot.first) }
    var reminderMinuteOfDay by rememberSaveable { mutableIntStateOf(initialReminderSlot.second) }
    var reminderIsPrivate by rememberSaveable { mutableStateOf(false) }

    val startCreate = {
        editingId = null
        editorDateMillis = Dates.startOfDay(Dates.today())
        editorMoodName = DiaryMood.CALM.name
        editorTitle = ""
        editorBody = ""
        editorOpen = true
    }

    val startEdit: (DiaryEntry) -> Unit = { entry ->
        editingId = entry.id
        editorDateMillis = entry.dateMillis
        editorMoodName = entry.mood.name
        editorTitle = entry.title
        editorBody = entry.body
        editorOpen = true
    }

    val startCreateReminder = {
        val (dateMillis, minuteOfDay) = defaultReminderSlot()
        editingReminderId = null
        reminderText = ""
        reminderDateMillis = dateMillis
        reminderMinuteOfDay = minuteOfDay
        // По умолчанию напоминание общее: смысл дневника — делиться с партнёром.
        reminderIsPrivate = false
        reminderEditorOpen = true
    }

    val startEditReminder: (DiaryReminder) -> Unit = { reminder ->
        val zoned = Instant.ofEpochMilli(reminder.remindAt).atZone(ZoneId.systemDefault())
        editingReminderId = reminder.id
        reminderText = reminder.text
        reminderDateMillis = Dates.startOfDay(zoned.toLocalDate())
        reminderMinuteOfDay = zoned.hour * 60 + zoned.minute
        reminderIsPrivate = reminder.isPrivate
        reminderEditorOpen = true
    }

    val reminderAt = reminderAtMillis(reminderDateMillis, reminderMinuteOfDay)
    val reminderInPast = reminderAt <= System.currentTimeMillis()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = VdvoemSpacing.screenHorizontal,
                end = VdvoemSpacing.screenHorizontal,
                top = VdvoemSpacing.sm,
                // FAB не должен закрывать последнюю карточку.
                bottom = VdvoemSpacing.listBottom
            ),
            verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
        ) {
            item(key = "hero") {
                DiaryHero()
            }

            item(key = "reminders_header") {
                SectionHeader(
                    title = stringResource(R.string.diary_section_reminders),
                    modifier = Modifier.padding(top = VdvoemSpacing.xs)
                )
            }

            item(key = "reminders_add") {
                FilledTonalButton(onClick = startCreateReminder) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(VdvoemSizes.iconMd)
                    )
                    Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
                    Text(stringResource(R.string.diary_reminder_add))
                }
            }

            if (state.reminders.isEmpty()) {
                item(key = "reminders_empty") {
                    Text(
                        text = stringResource(R.string.diary_reminder_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.reminders, key = { "reminder_${it.id}" }) { reminder ->
                    DiaryReminderCard(
                        reminder = reminder,
                        onOpen = { startEditReminder(reminder) },
                        onDone = { onReminderDone(reminder.id, true) }
                    )
                }
            }

            item(key = "header") {
                SectionHeader(
                    title = stringResource(R.string.diary_section_entries),
                    modifier = Modifier.padding(top = VdvoemSpacing.xs),
                    trailing = if (state.entries.isNotEmpty()) {
                        {
                            InfoPill(
                                text = pluralStringResource(
                                    R.plurals.diary_entries_count,
                                    state.entries.size,
                                    state.entries.size
                                )
                            )
                        }
                    } else {
                        null
                    }
                )
            }

            when {
                state.loading -> item(key = "loading") {
                    Text(
                        text = stringResource(R.string.diary_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = VdvoemSpacing.xl)
                    )
                }

                state.entries.isEmpty() -> item(key = "empty") {
                    EmptyState(
                        icon = Icons.Filled.Edit,
                        title = stringResource(R.string.diary_empty_title),
                        message = stringResource(R.string.diary_empty_message),
                        actionLabel = stringResource(R.string.diary_empty_action),
                        onAction = startCreate
                    )
                }

                else -> items(state.entries, key = { it.id }) { entry ->
                    DiaryEntryCard(
                        entry = entry,
                        onOpen = { startEdit(entry) },
                        onDelete = { pendingDeleteId = entry.id }
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = startCreate,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(VdvoemSpacing.lg)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(VdvoemSizes.iconMd)
            )
            Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
            Text(stringResource(R.string.diary_add))
        }
    }

    if (editorOpen) {
        DiaryEditorSheet(
            isNew = editingId == null,
            dateMillis = editorDateMillis,
            mood = DiaryMood.entries.firstOrNull { it.name == editorMoodName } ?: DiaryMood.CALM,
            title = editorTitle,
            body = editorBody,
            onDateChange = { editorDateMillis = it },
            onMoodChange = { editorMoodName = it.name },
            onTitleChange = { editorTitle = it },
            onBodyChange = { editorBody = it },
            onDismiss = { editorOpen = false },
            onSave = {
                onSave(editingId, editorDateMillis, editorMoodOf(editorMoodName), editorTitle, editorBody)
                editorOpen = false
            }
        )
    }

    if (reminderEditorOpen) {
        ReminderEditorSheet(
            isNew = editingReminderId == null,
            text = reminderText,
            dateMillis = reminderDateMillis,
            minuteOfDay = reminderMinuteOfDay,
            isPrivate = reminderIsPrivate,
            inPast = reminderInPast,
            onTextChange = { reminderText = it },
            onDateChange = { reminderDateMillis = it },
            onTimeChange = { reminderMinuteOfDay = it },
            onPrivateChange = { reminderIsPrivate = it },
            onDismiss = { reminderEditorOpen = false },
            onSave = {
                onSaveReminder(editingReminderId, reminderText, reminderAt, reminderIsPrivate)
                reminderEditorOpen = false
            },
            onDelete = {
                editingReminderId?.let(onDeleteReminder)
                reminderEditorOpen = false
            }
        )
    }

    pendingDeleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text(stringResource(R.string.diary_delete_title)) },
            text = { Text(stringResource(R.string.diary_delete_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(id)
                        pendingDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.diary_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text(stringResource(R.string.diary_cancel))
                }
            }
        )
    }
}

/** Hero: что это за экран и почему записи общие. Главное действие — FAB. */
@Composable
private fun DiaryHero() {
    VdvoemCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
        Text(
            text = stringResource(R.string.diary_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.height(VdvoemSpacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Сердечко декоративное: смысл несёт текст рядом.
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(VdvoemSizes.iconSm)
            )
            Spacer(modifier = Modifier.size(VdvoemSpacing.xs))
            Text(
                text = stringResource(R.string.diary_subtitle),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.height(VdvoemSpacing.xs))
        Text(
            text = stringResource(R.string.diary_hero_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun DiaryEntryCard(
    entry: DiaryEntry,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val dateLabel = entryDateLabel(entry.dateMillis)
    // Для TalkBack дата полная: «Удалить запись за Сегодня» звучит криво.
    val deleteLabel = stringResource(
        R.string.diary_card_delete,
        Dates.format(Dates.toLocalDate(entry.dateMillis))
    )
    VdvoemCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InfoPill(text = stringResource(entry.mood.labelRes()))
            Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = deleteLabel,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(VdvoemSizes.iconMd)
                )
            }
        }
        if (entry.title.isNotBlank()) {
            Spacer(modifier = Modifier.height(VdvoemSpacing.xs))
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (entry.body.isNotBlank()) {
            Spacer(modifier = Modifier.height(VdvoemSpacing.xs))
            Text(
                text = entry.body,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Карточка напоминания: текст, момент и отметка личного.
 *
 * Клик открывает редактор — удаление живёт там же, чтобы карточку нельзя было
 * снести одним неосторожным касанием.
 */
@Composable
private fun DiaryReminderCard(
    reminder: DiaryReminder,
    onOpen: () -> Unit,
    onDone: () -> Unit
) {
    VdvoemCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (reminder.isPrivate) Icons.Filled.Lock else Icons.Filled.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(VdvoemSizes.iconMd)
            )
            Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
            Text(
                text = reminder.text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDone) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.diary_reminder_done),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(VdvoemSizes.iconMd)
                )
            }
        }
        Spacer(modifier = Modifier.height(VdvoemSpacing.xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = reminderDateLabel(reminder.remindAt),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (reminder.isPrivate) {
                InfoPill(text = stringResource(R.string.diary_reminder_private))
            }
        }
    }
}

/**
 * Редактор напоминания в нижней шторке.
 *
 * Дата и время выбираются отдельно: DatePicker отдаёт только день, а напоминанию нужен
 * точный момент, поэтому время хранится минутами от полуночи и собирается в epoch millis
 * уже в поясе устройства.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderEditorSheet(
    isNew: Boolean,
    text: String,
    dateMillis: Long,
    minuteOfDay: Int,
    isPrivate: Boolean,
    inPast: Boolean,
    onTextChange: (String) -> Unit,
    onDateChange: (Long) -> Unit,
    onTimeChange: (Int) -> Unit,
    onPrivateChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    // Режим выбора времени: циферблат или ручной ввод с клавиатуры.
    var manualTime by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val textIntoView = remember { BringIntoViewRequester() }
    val date = Dates.toLocalDate(dateMillis)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Клавиатура сжимает видимую часть шторки: imePadding снаружи прокрутки,
                // и поле остаётся в кадре.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VdvoemSpacing.screenHorizontal)
                .padding(bottom = VdvoemSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.md)
        ) {
            Text(
                text = stringResource(
                    if (isNew) R.string.diary_reminder_editor_new else R.string.diary_reminder_editor_edit
                ),
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                label = { Text(stringResource(R.string.diary_reminder_field_text)) },
                placeholder = { Text(stringResource(R.string.diary_reminder_field_text_hint)) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .revealOnFocus(textIntoView, scope)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(VdvoemSizes.iconSm)
                    )
                    Spacer(modifier = Modifier.size(VdvoemSpacing.xs))
                    Text(text = date.format(shortDateFormat), maxLines = 1)
                }
                OutlinedButton(
                    // Каждый раз начинаем с циферблата: ручной ввод — осознанный выбор,
                    // а не режим, который «залип» с прошлого открытия.
                    onClick = {
                        manualTime = false
                        showTimePicker = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(
                            R.string.diary_reminder_time_label,
                            formatReminderTime(minuteOfDay)
                        ),
                        maxLines = 1
                    )
                }
            }

            if (inPast) {
                Text(
                    text = stringResource(R.string.diary_reminder_past),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = isPrivate, onCheckedChange = onPrivateChange)
                Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.diary_reminder_field_private),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.diary_reminder_private_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isNew) {
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.diary_delete_confirm))
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.diary_cancel))
                }
                Button(
                    onClick = onSave,
                    // Пустой текст и прошедшее время сохранять бессмысленно.
                    enabled = text.isNotBlank() && !inPast
                ) {
                    Text(stringResource(R.string.diary_save))
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = Dates.toPickerMillis(dateMillis)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { onDateChange(Dates.fromPickerMillis(it)) }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.diary_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.diary_cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = minuteOfDay / 60,
            initialMinute = minuteOfDay % 60,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.diary_reminder_time_title)) },
            text = {
                val timeInputLabel = stringResource(R.string.diary_reminder_time_input)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (manualTime) {
                        TimeInput(state = timeState)
                    } else {
                        Box {
                            TimePicker(state = timeState)
                            // Шапка циферблата (часы и минуты) занимает 80.dp, до самого
                            // циферблата ещё 36.dp отступа. Прозрачная зона покрывает только
                            // шапку, поэтому касание цифр времени переключает на ввод с клавиатуры.
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .height(96.dp)
                                    // Экранный диктор должен понимать, что шапка времени нажимаемая.
                                    .semantics {
                                        contentDescription = timeInputLabel
                                    }
                                    .clickable { manualTime = true }
                            )
                        }
                    }
                    TextButton(onClick = { manualTime = !manualTime }) {
                        Text(
                            text = stringResource(
                                if (manualTime) {
                                    R.string.diary_reminder_time_dial
                                } else {
                                    R.string.diary_reminder_time_input
                                }
                            )
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeChange(timeState.hour * 60 + timeState.minute)
                        showTimePicker = false
                    }
                ) {
                    Text(stringResource(R.string.diary_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.diary_cancel))
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.diary_reminder_delete_title)) },
            text = { Text(stringResource(R.string.diary_reminder_delete_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.diary_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.diary_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DiaryEditorSheet(
    isNew: Boolean,
    dateMillis: Long,
    mood: DiaryMood,
    title: String,
    body: String,
    onDateChange: (Long) -> Unit,
    onMoodChange: (DiaryMood) -> Unit,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // Поле не должно оставаться под клавиатурой: после её появления просим шторку
    // прокрутиться к тому полю, на которое пользователь нажал.
    val titleIntoView = remember { BringIntoViewRequester() }
    val bodyIntoView = remember { BringIntoViewRequester() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Клавиатура сжимает видимую часть шторки, а не закрывает поля:
                // imePadding снаружи прокрутки, поэтому поля остаются в кадре.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VdvoemSpacing.screenHorizontal)
                .padding(bottom = VdvoemSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.md)
        ) {
            Text(
                text = stringResource(
                    if (isNew) R.string.diary_editor_new else R.string.diary_editor_edit
                ),
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedButton(
                onClick = { showPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(VdvoemSizes.iconMd)
                )
                Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
                Text(
                    text = stringResource(
                        R.string.diary_field_date,
                        Dates.format(Dates.toLocalDate(dateMillis))
                    )
                )
            }

            Text(
                text = stringResource(R.string.diary_field_mood),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.xs)
            ) {
                DiaryMood.entries.forEach { item ->
                    FilterChip(
                        selected = item == mood,
                        onClick = { onMoodChange(item) },
                        label = { Text(stringResource(item.labelRes())) }
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                singleLine = true,
                label = { Text(stringResource(R.string.diary_field_title)) },
                placeholder = { Text(stringResource(R.string.diary_field_title_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .revealOnFocus(titleIntoView, scope)
            )

            OutlinedTextField(
                value = body,
                onValueChange = onBodyChange,
                label = { Text(stringResource(R.string.diary_field_body)) },
                placeholder = { Text(stringResource(R.string.diary_field_body_hint)) },
                minLines = 4,
                maxLines = 8,
                modifier = Modifier
                    .fillMaxWidth()
                    .revealOnFocus(bodyIntoView, scope)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.diary_cancel))
                }
                Button(
                    onClick = onSave,
                    // Пустая запись бессмысленна — сохранять нечего.
                    enabled = body.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.diary_save))
                }
            }
        }
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = Dates.toPickerMillis(dateMillis)
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { onDateChange(Dates.fromPickerMillis(it)) }
                        showPicker = false
                    }
                ) {
                    Text(stringResource(R.string.diary_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.diary_cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/**
 * Просит шторку прокрутиться к полю после появления клавиатуры.
 *
 * Сразу после нажатия поле ещё считается видимым, а IME «съедает» его позже,
 * поэтому ждём окончания анимации и только потом просим показать поле.
 */
@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.revealOnFocus(
    requester: BringIntoViewRequester,
    scope: CoroutineScope
): Modifier = bringIntoViewRequester(requester).onFocusChanged { focus ->
    if (focus.isFocused) {
        scope.launch {
            delay(300)
            requester.bringIntoView()
        }
    }
}

/** «Сегодня», «Вчера» или полная дата — как в календаре. */
@Composable
private fun entryDateLabel(dateMillis: Long): String {
    val date = Dates.toLocalDate(dateMillis)
    val today = Dates.today()
    return when (date) {
        today -> stringResource(R.string.diary_today)
        today.minusDays(1) -> stringResource(R.string.diary_yesterday)
        else -> Dates.format(date)
    }
}

/** «Сегодня, 19:30», «Завтра, 09:00» или «22 ноября, 09:00». */
@Composable
private fun reminderDateLabel(remindAt: Long): String {
    val zoned = Instant.ofEpochMilli(remindAt).atZone(ZoneId.systemDefault())
    val time = zoned.format(reminderTimeFormat)
    val today = LocalDate.now()
    return when (zoned.toLocalDate()) {
        today -> stringResource(R.string.diary_reminder_today, time)
        today.plusDays(1) -> stringResource(R.string.diary_reminder_tomorrow, time)
        else -> zoned.format(reminderCardDateFormat)
    }
}

private val shortDateFormat: DateTimeFormatter
    get() = DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault())

private val reminderTimeFormat: DateTimeFormatter
    get() = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

private val reminderCardDateFormat: DateTimeFormatter
    get() = DateTimeFormatter.ofPattern("d MMMM, HH:mm", Locale.getDefault())

private fun formatReminderTime(minuteOfDay: Int): String =
    LocalTime.of(minuteOfDay / 60, minuteOfDay % 60).format(reminderTimeFormat)

/** Собирает день и время в точный момент по поясу устройства. */
private fun reminderAtMillis(dateMillis: Long, minuteOfDay: Int): Long =
    Dates.toLocalDate(dateMillis)
        .atTime(LocalTime.of(minuteOfDay / 60, minuteOfDay % 60))
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

/**
 * Значения по умолчанию для нового напоминания: ближайший круглый час в будущем.
 * Если это уже полночь следующих суток — утро, 09:00: напоминание «в 24:00» никому не нужно.
 */
private fun defaultReminderSlot(): Pair<Long, Int> {
    val now = LocalDateTime.now()
    val next = now.plusHours(1).withMinute(0).withSecond(0).withNano(0)
    return if (next.hour > 0) {
        Dates.startOfDay(next.toLocalDate()) to next.hour * 60
    } else {
        Dates.startOfDay(now.toLocalDate().plusDays(1)) to MORNING_MINUTE_OF_DAY
    }
}

private const val MORNING_MINUTE_OF_DAY = 9 * 60

private fun DiaryMood.labelRes(): Int = when (this) {
    DiaryMood.JOY -> R.string.diary_mood_joy
    DiaryMood.CALM -> R.string.diary_mood_calm
    DiaryMood.NEUTRAL -> R.string.diary_mood_neutral
    DiaryMood.SAD -> R.string.diary_mood_sad
    DiaryMood.TIRED -> R.string.diary_mood_tired
}

private fun editorMoodOf(name: String): DiaryMood =
    DiaryMood.entries.firstOrNull { it.name == name } ?: DiaryMood.CALM

private val previewEntry = DiaryEntry(
    id = "preview",
    pairId = "local",
    dateMillis = Dates.startOfDay(Dates.today()),
    mood = DiaryMood.JOY,
    title = "Тихий вечер вдвоём",
    body = "Готовили пасту, слушали старые песни и почти не смотрели в телефоны."
)

private val previewEntryWithoutTitle = DiaryEntry(
    id = "preview-2",
    pairId = "local",
    dateMillis = Dates.startOfDay(Dates.today().minusDays(1)),
    mood = DiaryMood.TIRED,
    title = "",
    body = "Долгий день, но вечером успели погулять перед сном."
)

private val previewReminder = DiaryReminder(
    id = "preview-reminder",
    pairId = "local",
    text = "Купить цветы к ужину",
    remindAt = reminderAtMillis(Dates.startOfDay(Dates.today()), 19 * 60)
)

private val previewPrivateReminder = DiaryReminder(
    id = "preview-reminder-2",
    pairId = "local",
    text = "Позвонить маме",
    remindAt = reminderAtMillis(Dates.startOfDay(Dates.today().plusDays(1)), 9 * 60),
    isPrivate = true
)

@Preview(showBackground = true)
@Composable
private fun DiaryScreenPreview() {
    VdvoemTheme {
        DiaryScreen(
            state = DiaryUiState(
                entries = listOf(previewEntry, previewEntryWithoutTitle),
                reminders = listOf(previewReminder, previewPrivateReminder),
                loading = false
            ),
            onSave = { _, _, _, _, _ -> },
            onDelete = {},
            onSaveReminder = { _, _, _, _ -> },
            onDeleteReminder = {},
            onReminderDone = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DiaryScreenEmptyPreview() {
    VdvoemTheme {
        DiaryScreen(
            state = DiaryUiState(loading = false),
            onSave = { _, _, _, _, _ -> },
            onDelete = {},
            onSaveReminder = { _, _, _, _ -> },
            onDeleteReminder = {},
            onReminderDone = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DiaryEntryCardPreview() {
    VdvoemTheme {
        Column(modifier = Modifier.padding(VdvoemSpacing.screenHorizontal)) {
            DiaryEntryCard(entry = previewEntry, onOpen = {}, onDelete = {})
            Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
            DiaryEntryCard(entry = previewEntryWithoutTitle, onOpen = {}, onDelete = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DiaryReminderCardPreview() {
    VdvoemTheme {
        Column(modifier = Modifier.padding(VdvoemSpacing.screenHorizontal)) {
            DiaryReminderCard(reminder = previewReminder, onOpen = {}, onDone = {})
            Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
            DiaryReminderCard(reminder = previewPrivateReminder, onOpen = {}, onDone = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DiaryRemindersEmptyPreview() {
    VdvoemTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(VdvoemSpacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
        ) {
            SectionHeader(title = stringResource(R.string.diary_section_reminders))
            FilledTonalButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(VdvoemSizes.iconMd)
                )
                Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
                Text(stringResource(R.string.diary_reminder_add))
            }
            Text(
                text = stringResource(R.string.diary_reminder_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}