package app.vdvoem.feature.calendar

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import app.vdvoem.core.domain.time.Dates
import app.vdvoem.core.ui.components.EmptyState
import app.vdvoem.core.ui.components.SectionHeader
import app.vdvoem.core.ui.components.VdvoemCard
import app.vdvoem.core.ui.theme.VdvoemSizes
import app.vdvoem.core.ui.theme.VdvoemSpacing
import app.vdvoem.core.ui.theme.VdvoemTheme
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onFilterChange: (CalendarFilter) -> Unit,
    onSaveEvent: (id: String?, title: String, dateMillis: Long, annual: Boolean) -> Unit,
    onDeleteEvent: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<EventUi?>(null) }
    var pendingDelete by remember { mutableStateOf<EventUi?>(null) }

    val startCreate = {
        editing = null
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

            if (state.totalCount > 0) {
                item(key = "filters") {
                    CalendarFilterRow(
                        filter = state.filter,
                        onFilterChange = onFilterChange,
                        modifier = Modifier.padding(top = VdvoemSpacing.sm)
                    )
                }
            }

            item(key = "header") {
                SectionHeader(
                    title = stringResource(R.string.calendar_section_events),
                    modifier = Modifier.padding(top = VdvoemSpacing.sm),
                    trailing = {
                        TextButton(onClick = startCreate) {
                            Text(stringResource(R.string.calendar_action_add))
                        }
                    }
                )
            }

            when {
                state.totalCount == 0 -> item(key = "empty") {
                    EmptyState(
                        icon = Icons.Filled.DateRange,
                        title = stringResource(R.string.calendar_empty_title),
                        message = stringResource(R.string.calendar_empty_message),
                        actionLabel = stringResource(R.string.calendar_empty_action),
                        onAction = startCreate
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
                        onEdit = {
                            editing = event
                            editorOpen = true
                        },
                        onRequestDelete = { pendingDelete = event }
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = startCreate,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.calendar_fab)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = VdvoemSpacing.screenHorizontal,
                    bottom = VdvoemSpacing.xxl
                )
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
private fun CalendarHero(next: EventUi?) {
    VdvoemCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
        if (next == null) {
            Text(
                text = stringResource(R.string.calendar_hero_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
            Text(
                text = stringResource(R.string.calendar_hero_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            return@VdvoemCard
        }

        Text(
            text = stringResource(R.string.calendar_hero_next),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
        )
        Spacer(modifier = Modifier.size(VdvoemSpacing.xs))
        Text(
            text = next.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.size(VdvoemSpacing.md))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = next.daysLeft.toString(),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
            Text(
                text = pluralStringResource(
                    R.plurals.calendar_days_left,
                    next.daysLeft.toInt(),
                    next.daysLeft
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(bottom = VdvoemSpacing.sm)
            )
        }
        if (next.daysLeft == 0L) {
            Text(
                text = stringResource(R.string.calendar_today),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.size(VdvoemSpacing.xs))
        Text(
            text = Dates.format(next.date),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun CalendarFilterRow(
    filter: CalendarFilter,
    onFilterChange: (CalendarFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
    ) {
        CalendarFilter.entries.forEach { item ->
            FilterChip(
                selected = item == filter,
                onClick = { onFilterChange(item) },
                label = { Text(stringResource(item.labelRes())) }
            )
        }
    }
}

@Composable
private fun EventCard(
    event: EventUi,
    onEdit: () -> Unit,
    onRequestDelete: () -> Unit
) {
    VdvoemCard(modifier = Modifier.clickable(onClick = onEdit)) {
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
                    text = event.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (event.annual) {
                        stringResource(R.string.calendar_card_meta_annual, Dates.format(event.date))
                    } else {
                        Dates.format(event.date)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRequestDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.calendar_delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
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
    var title by remember(event?.id) { mutableStateOf(event?.title.orEmpty()) }
    var dateMillis by remember(event?.id) {
        mutableLongStateOf(event?.dateMillis ?: Dates.startOfDay(Dates.today()))
    }
    var annual by remember(event?.id) { mutableStateOf(event?.annual ?: false) }
    var showPicker by remember(event?.id) { mutableStateOf(false) }
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
                    Text(Dates.format(Dates.toLocalDate(dateMillis)))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.calendar_annual),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = annual, onCheckedChange = { annual = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, dateMillis, annual) },
                enabled = canSave
            ) {
                Text(stringResource(R.string.calendar_save))
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(VdvoemSizes.iconSm)
                        )
                        Spacer(modifier = Modifier.size(VdvoemSpacing.xs))
                        Text(stringResource(R.string.calendar_action_delete))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.calendar_cancel))
                }
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
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
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

@Composable
private fun CalendarFilter.labelRes(): Int = when (this) {
    CalendarFilter.ALL -> R.string.calendar_filter_all
    CalendarFilter.TODAY -> R.string.calendar_filter_today
    CalendarFilter.WEEK -> R.string.calendar_filter_week
    CalendarFilter.MONTH -> R.string.calendar_filter_month
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
                loading = false
            ),
            onFilterChange = {},
            onSaveEvent = { _, _, _, _ -> },
            onDeleteEvent = {},
            modifier = Modifier
        )
    }
}