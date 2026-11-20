package app.vdvoem.feature.calendar

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import app.vdvoem.core.domain.time.Dates
import app.vdvoem.core.ui.components.EmptyState
import app.vdvoem.core.ui.components.SectionHeader
import app.vdvoem.core.ui.components.VdvoemCard
import app.vdvoem.core.ui.theme.VdvoemSizes
import app.vdvoem.core.ui.theme.VdvoemSpacing
import app.vdvoem.core.ui.theme.VdvoemTheme
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onFilterChange: (CalendarFilter) -> Unit,
    onSortChange: (CalendarSort) -> Unit,
    onSaveEvent: (id: String?, title: String, dateMillis: Long, annual: Boolean) -> Unit,
    onDeleteEvent: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    onRemindersChange: (Boolean) -> Unit = {},
    onLeadDaysChange: (Int) -> Unit = {},
    onReminderHourChange: (Int) -> Unit = {}
) {
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val editing = state.events.firstOrNull { it.id == editingId }
    var pendingDelete by remember { mutableStateOf<EventUi?>(null) }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deniedMessage = stringResource(R.string.calendar_reminder_denied)
    val calendarFailedMessage = stringResource(R.string.calendar_add_to_calendar_failed)

    // Экспорт в календарь телефона — только через Intent, без OAuth и разрешений.
    val addToPhoneCalendar: (EventUi) -> Unit = { event ->
        // В профиле у дня рождения только имя: подпись «День рождения» добавляем здесь.
        val title = if (event.isBirthday) {
            context.getString(R.string.calendar_birthday_title, event.title)
        } else {
            event.title
        }
        if (!insertIntoPhoneCalendar(context, event, title)) {
            scope.launch { snackbarHostState.showSnackbar(calendarFailedMessage) }
        }
    }

    // Разрешение на уведомления: без него напоминания молча не покажутся.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onRemindersChange(granted)
        if (!granted) {
            scope.launch { snackbarHostState.showSnackbar(deniedMessage) }
        }
    }

    val toggleReminders: (Boolean) -> Unit = { enabled ->
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        when {
            !enabled -> onRemindersChange(false)
            needsPermission -> permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else -> onRemindersChange(true)
        }
    }

    val startCreate = {
        editingId = null
        editorOpen = true
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = VdvoemSpacing.screenHorizontal,
                end = VdvoemSpacing.screenHorizontal,
                top = VdvoemSpacing.lg,
                bottom = VdvoemSpacing.listBottom
            ),
            verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.md)
        ) {
            item(key = "hero") {
                CalendarHero(next = state.next)
            }

            item(key = "reminders") {
                RemindersCard(
                    settings = state.reminders,
                    onToggle = toggleReminders,
                    onLeadDaysChange = onLeadDaysChange,
                    onHourChange = onReminderHourChange
                )
            }

            if (state.totalCount > 0) {
                item(key = "filters") {
                    CalendarPeriodChips(
                        filter = state.filter,
                        onFilterChange = onFilterChange
                    )
                }
            }

            item(key = "header") {
                SectionHeader(
                    title = stringResource(R.string.calendar_section_events),
                    modifier = Modifier.padding(top = VdvoemSpacing.xs),
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (state.filter != CalendarFilter.ALL) {
                                TextButton(onClick = { onFilterChange(CalendarFilter.ALL) }) {
                                    Text(stringResource(R.string.calendar_show_all))
                                }
                            }
                            SortMenuButton(sort = state.sort, onSortChange = onSortChange)
                        }
                    }
                )
            }

            when {
                state.loading -> item(key = "loading") {
                    HintText(stringResource(R.string.calendar_loading))
                }

                state.totalCount == 0 -> item(key = "empty") {
                    EmptyState(
                        icon = Icons.Filled.DateRange,
                        title = stringResource(R.string.calendar_empty_title),
                        message = stringResource(R.string.calendar_empty_message)
                    )
                }

                state.events.isEmpty() -> item(key = "empty_filter") {
                    EmptyState(
                        icon = Icons.Filled.DateRange,
                        title = stringResource(R.string.calendar_filter_empty_title),
                        message = stringResource(R.string.calendar_filter_empty_message),
                        actionLabel = stringResource(R.string.calendar_filter_reset),
                        onAction = { onFilterChange(CalendarFilter.ALL) }
                    )
                }

                else -> items(state.events, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        onAddToCalendar = { addToPhoneCalendar(event) },
                        onEdit = {
                            editingId = event.id
                            editorOpen = true
                        }
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
            Text(stringResource(R.string.calendar_add))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = VdvoemSpacing.lg, vertical = VdvoemSpacing.listBottom)
        )
    }

    if (editorOpen) {
        val target = editing
        EventDialog(
            event = target,
            onDismiss = { editorOpen = false },
            onSave = { title, dateMillis, annual ->
                onSaveEvent(target?.id, title, dateMillis, annual)
                editorOpen = false
            },
            onDelete = target?.let {
                {
                    editorOpen = false
                    pendingDelete = it
                }
            }
        )
    }

    pendingDelete?.let { target ->
        DeleteEventDialog(
            title = target.title,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                onDeleteEvent(target.id)
                pendingDelete = null
            }
        )
    }
}

@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = VdvoemSpacing.xl)
    )
}

/** Компактный hero: ближайшее событие или призыв добавить дату. Главное действие — FAB. */
@Composable
private fun CalendarHero(next: EventUi?) {
    VdvoemCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
        Text(
            text = stringResource(R.string.calendar_hero_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.size(VdvoemSpacing.xs))
        Text(
            text = if (next != null) {
                stringResource(R.string.calendar_hero_next, eventTitle(next), next.relativeLabel())
            } else {
                stringResource(R.string.calendar_hero_empty)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Переключатель напоминаний и их расписание: за сколько дней и в котором часу. */
@Composable
private fun RemindersCard(
    settings: ReminderSettings,
    onToggle: (Boolean) -> Unit,
    onLeadDaysChange: (Int) -> Unit,
    onHourChange: (Int) -> Unit
) {
    VdvoemCard(onClick = { onToggle(!settings.enabled) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.calendar_reminder_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(
                        R.string.calendar_reminder_hint_custom,
                        settings.leadDays,
                        settings.hour
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
            Switch(checked = settings.enabled, onCheckedChange = onToggle)
        }

        if (settings.enabled) {
            Text(
                text = stringResource(R.string.calendar_reminder_lead_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = VdvoemSpacing.sm)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)) {
                ReminderSettings.LEAD_OPTIONS.forEach { days ->
                    FilterChip(
                        selected = days == settings.leadDays,
                        onClick = { onLeadDaysChange(days) },
                        label = { Text(stringResource(R.string.calendar_reminder_lead_days, days)) }
                    )
                }
            }
            Text(
                text = stringResource(R.string.calendar_reminder_hour_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = VdvoemSpacing.xs)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)) {
                ReminderSettings.HOUR_OPTIONS.forEach { hour ->
                    FilterChip(
                        selected = hour == settings.hour,
                        onClick = { onHourChange(hour) },
                        label = { Text(stringResource(R.string.calendar_reminder_hour, hour)) }
                    )
                }
            }
        }
    }
}

/** Периоды — одна прокручиваемая строка: без переносов и без второй строки. */
@Composable
private fun CalendarPeriodChips(
    filter: CalendarFilter,
    onFilterChange: (CalendarFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
    ) {
        CalendarFilter.entries.forEach { item ->
            FilterChip(
                selected = item == filter,
                onClick = { onFilterChange(item) },
                label = { Text(stringResource(item.labelRes())) },
                leadingIcon = if (item == filter) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(VdvoemSizes.iconSm)
                        )
                    }
                } else {
                    null
                }
            )
        }
    }
}

/** Сортировка — иконка в шапке секции; название текущего порядка в описании. */
@Composable
private fun SortMenuButton(
    sort: CalendarSort,
    onSortChange: (CalendarSort) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = stringResource(
                    R.string.calendar_sort_label,
                    stringResource(sort.labelRes())
                ),
                modifier = Modifier.size(VdvoemSizes.iconMd)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CalendarSort.entries.forEach { item ->
                DropdownMenuItem(
                    text = { Text(stringResource(item.labelRes())) },
                    onClick = {
                        onSortChange(item)
                        expanded = false
                    },
                    trailingIcon = {
                        if (item == sort) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(VdvoemSizes.iconMd)
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun EventCard(
    event: EventUi,
    onAddToCalendar: () -> Unit,
    onEdit: () -> Unit
) {
    val title = eventTitle(event)
    val editHint = stringResource(R.string.calendar_card_edit_hint, title)
    // День рождения приходит из профиля пары: править его в календаре нечего.
    VdvoemCard(onClick = if (event.isBirthday) null else onEdit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(VdvoemSizes.heroBadge)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = event.date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = shortMonth(event),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.size(VdvoemSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    // У дня рождения длинная подпись — даём ей вторую строку,
                    // иначе «День рождения: Партнёр» обрезается.
                    maxLines = if (event.isBirthday) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = event.metaLine(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
            IconButton(onClick = onAddToCalendar) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = stringResource(R.string.calendar_add_to_calendar),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(VdvoemSizes.iconMd)
                )
            }
            if (!event.isBirthday) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = editHint,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(VdvoemSizes.iconLg)
                )
            }
        }
    }
}

/** В профиле пары у дня рождения только имя — подпись добавляем при показе. */
@Composable
private fun eventTitle(event: EventUi): String =
    if (event.isBirthday) {
        stringResource(R.string.calendar_birthday_title, event.title)
    } else {
        event.title
    }

/** «Через 5 дней · 1 января 2025 · каждый год» — без догадок, что именно за дата. */
@Composable
private fun EventUi.metaLine(): String {
    val relative = relativeLabel()
    val date = Dates.format(date)
    return if (annual) {
        stringResource(R.string.calendar_card_meta_annual, relative, date)
    } else {
        stringResource(R.string.calendar_card_meta, relative, date)
    }
}

@Composable
private fun EventUi.relativeLabel(): String = when {
    daysLeft == 0L -> stringResource(R.string.calendar_today)
    daysLeft > 0L -> stringResource(
        R.string.calendar_in_days,
        pluralStringResource(R.plurals.calendar_days_left, daysLeft.toInt(), daysLeft)
    )

    else -> stringResource(R.string.calendar_passed)
}

private fun shortMonth(event: EventUi): String =
    event.date.format(DateTimeFormatter.ofPattern("LLL", Locale("ru")))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDialog(
    event: EventUi?,
    onDismiss: () -> Unit,
    onSave: (title: String, dateMillis: Long, annual: Boolean) -> Unit,
    onDelete: (() -> Unit)?
) {
    var title by rememberSaveable(event?.id) { mutableStateOf(event?.title.orEmpty()) }
    var dateMillis by rememberSaveable(event?.id) {
        mutableLongStateOf(event?.dateMillis ?: Dates.startOfDay(Dates.today()))
    }
    var annual by rememberSaveable(event?.id) { mutableStateOf(event?.annual ?: false) }
    var showPicker by rememberSaveable(event?.id) { mutableStateOf(false) }
    val canSave = title.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (event == null) R.string.calendar_dialog_title
                    else R.string.calendar_dialog_edit_title
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.md)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.calendar_field_title)) },
                    placeholder = { Text(stringResource(R.string.calendar_field_title_hint)) },
                    modifier = Modifier.fillMaxWidth()
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
                            R.string.calendar_field_date,
                            Dates.format(Dates.toLocalDate(dateMillis))
                        )
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.calendar_annual),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(R.string.calendar_annual_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = annual, onCheckedChange = { annual = it })
                }
                if (onDelete != null) {
                    Spacer(modifier = Modifier.size(VdvoemSpacing.xs))
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(VdvoemSizes.iconMd)
                        )
                        Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
                        Text(stringResource(R.string.calendar_action_delete))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, dateMillis, annual) },
                enabled = canSave
            ) {
                Text(stringResource(R.string.calendar_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.calendar_cancel))
            }
        }
    )

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = Dates.toPickerMillis(dateMillis)
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let {
                            dateMillis = Dates.fromPickerMillis(it)
                        }
                        showPicker = false
                    }
                ) {
                    Text(stringResource(R.string.calendar_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.calendar_cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun DeleteEventDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.calendar_delete_title)) },
        text = { Text(stringResource(R.string.calendar_delete_message, title)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(R.string.calendar_delete_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.calendar_cancel))
            }
        }
    )
}

private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

/**
 * Кладёт событие в календарь телефона через ACTION_INSERT: без OAuth, без разрешений и без записи
 * в базу календаря — дальше пользователь сам жмёт «Сохранить» в приложении календаря.
 * В модели нет времени события, поэтому запись создаётся как событие на весь день.
 */
private fun insertIntoPhoneCalendar(context: Context, event: EventUi, title: String): Boolean {
    val begin = Dates.startOfDay(event.date)
    val description = context.applicationInfo?.loadLabel(context.packageManager)?.toString().orEmpty()
    val intent = Intent(Intent.ACTION_INSERT)
        .setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, begin + MILLIS_PER_DAY)
        .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
        .putExtra(CalendarContract.Events.TITLE, title)
        .putExtra(CalendarContract.Events.DESCRIPTION, description)
    // context из LocalContext — это Activity, поэтому FLAG_ACTIVITY_NEW_TASK не нужен.
    return runCatching { context.startActivity(intent) }.isSuccess
}

private fun CalendarFilter.labelRes(): Int = when (this) {
    CalendarFilter.ALL -> R.string.calendar_filter_all
    CalendarFilter.TODAY -> R.string.calendar_filter_today
    CalendarFilter.WEEK -> R.string.calendar_filter_week
    CalendarFilter.MONTH -> R.string.calendar_filter_month
}

private fun CalendarSort.labelRes(): Int = when (this) {
    CalendarSort.NEAREST -> R.string.calendar_sort_nearest
    CalendarSort.ALPHABET -> R.string.calendar_sort_alphabet
}

@Preview(showBackground = true)
@Composable
private fun CalendarScreenPreview() {
    val today = Dates.today()
    VdvoemTheme {
        CalendarScreen(
            state = CalendarUiState(
                events = listOf(
                    EventUi(
                        id = "1",
                        pairId = "local",
                        title = "Годовщина",
                        date = today.plusDays(5),
                        dateMillis = Dates.startOfDay(today.minusYears(2)),
                        annual = true,
                        daysLeft = 5
                    ),
                    EventUi(
                        id = "2",
                        pairId = "local",
                        title = "Поездка в Карелию",
                        date = today.plusDays(24),
                        dateMillis = Dates.startOfDay(today.plusDays(24)),
                        annual = false,
                        daysLeft = 24
                    )
                ),
                totalCount = 2,
                filter = CalendarFilter.ALL,
                sort = CalendarSort.NEAREST,
                remindersEnabled = true,
                next = EventUi(
                    id = "1",
                    pairId = "local",
                    title = "Годовщина",
                    date = today.plusDays(5),
                    dateMillis = Dates.startOfDay(today.minusYears(2)),
                    annual = true,
                    daysLeft = 5
                ),
                loading = false
            ),
            onFilterChange = {},
            onSortChange = {},
            onSaveEvent = { _, _, _, _ -> },
            onDeleteEvent = {},
            modifier = Modifier
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarScreenEmptyPreview() {
    VdvoemTheme {
        CalendarScreen(
            state = CalendarUiState(loading = false),
            onFilterChange = {},
            onSortChange = {},
            onSaveEvent = { _, _, _, _ -> },
            onDeleteEvent = {},
            modifier = Modifier
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RemindersCardPreview() {
    VdvoemTheme {
        Column(modifier = Modifier.padding(VdvoemSpacing.screenHorizontal)) {
            RemindersCard(
                settings = ReminderSettings(enabled = true),
                onToggle = {},
                onLeadDaysChange = {},
                onHourChange = {}
            )
            Spacer(modifier = Modifier.size(VdvoemSpacing.md))
            RemindersCard(
                settings = ReminderSettings(enabled = false),
                onToggle = {},
                onLeadDaysChange = {},
                onHourChange = {}
            )
        }
    }
}