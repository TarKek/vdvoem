package app.vdvoem.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.vdvoem.R
import app.vdvoem.core.domain.model.CalendarEvent
import app.vdvoem.core.domain.model.Pair
import app.vdvoem.core.domain.module.AppModule
import app.vdvoem.core.domain.time.Dates
import app.vdvoem.core.ui.components.EmptyState
import app.vdvoem.core.ui.components.InfoPill
import app.vdvoem.core.ui.components.SectionHeader
import app.vdvoem.core.ui.components.VdvoemCard
import app.vdvoem.core.ui.theme.VdvoemSizes
import app.vdvoem.core.ui.theme.VdvoemSpacing

@Composable
fun DashboardScreen(
    modules: List<AppModule>,
    pair: Pair?,
    nextEvent: CalendarEvent?,
    onOpenModule: (AppModule) -> Unit,
    onOpenStore: () -> Unit,
    onSavePair: (nameA: String, nameB: String, sinceMillis: Long) -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPairDialog by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = VdvoemSpacing.screenHorizontal,
            end = VdvoemSpacing.screenHorizontal,
            top = VdvoemSpacing.lg,
            bottom = VdvoemSpacing.listBottom
        ),
        verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.md)
    ) {
        item(key = "hero") {
            HeroCard(
                pair = pair,
                onEditPair = { showPairDialog = true }
            )
        }

        item(key = "modules_header") {
            SectionHeader(
                title = stringResource(R.string.dashboard_modules_title),
                modifier = Modifier.padding(top = VdvoemSpacing.sm),
                trailing = {
                    TextButton(onClick = onOpenStore) {
                        Text(text = stringResource(R.string.dashboard_modules_all))
                    }
                }
            )
        }

        if (modules.isEmpty()) {
            item(key = "modules_empty") {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.List,
                    title = stringResource(R.string.dashboard_modules_empty_title),
                    message = stringResource(R.string.dashboard_modules_empty_message),
                    actionLabel = stringResource(R.string.dashboard_open_store),
                    onAction = onOpenStore
                )
            }
        } else {
            items(modules, key = { it.manifest.id }) { module ->
                ModuleQuickRow(module = module, onClick = { onOpenModule(module) })
            }
        }

        item(key = "upcoming_header") {
            SectionHeader(
                title = stringResource(R.string.dashboard_upcoming_title),
                modifier = Modifier.padding(top = VdvoemSpacing.lg)
            )
        }

        if (nextEvent == null) {
            item(key = "upcoming_empty") {
                EmptyState(
                    icon = Icons.Filled.DateRange,
                    title = stringResource(R.string.dashboard_upcoming_empty_title),
                    message = stringResource(R.string.dashboard_upcoming_empty_message),
                    actionLabel = stringResource(R.string.dashboard_upcoming_empty_action),
                    onAction = onOpenCalendar
                )
            }
        } else {
            item(key = "upcoming_event") {
                UpcomingEventCard(event = nextEvent, onClick = onOpenCalendar)
            }
        }
    }

    if (showPairDialog) {
        PairDialog(
            pair = pair,
            onDismiss = { showPairDialog = false },
            onConfirm = { nameA, nameB, sinceMillis ->
                onSavePair(nameA, nameB, sinceMillis)
                showPairDialog = false
            }
        )
    }
}

@Composable
private fun HeroCard(pair: Pair?, onEditPair: () -> Unit) {
    VdvoemCard(containerColor = MaterialTheme.colorScheme.primary) {
        Text(
            text = if (pair == null) stringResource(R.string.dashboard_hero_title)
            else stringResource(R.string.dashboard_pair_names, pair.nameA, pair.nameB),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
        Text(
            text = if (pair == null) stringResource(R.string.dashboard_hero_text)
            else stringResource(R.string.dashboard_pair_since, Dates.format(Dates.toLocalDate(pair.sinceMillis))),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f)
        )
        if (pair != null) {
            val days = Dates.daysUntil(Dates.today(), Dates.toLocalDate(pair.sinceMillis))
            if (days > 0) {
                Spacer(modifier = Modifier.size(VdvoemSpacing.md))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = pluralStringResource(R.plurals.dashboard_days_together, days.toInt(), days),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(
                            horizontal = VdvoemSpacing.md,
                            vertical = VdvoemSpacing.sm
                        )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.size(VdvoemSpacing.xl))
        FilledTonalButton(
            onClick = onEditPair,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.onPrimary,
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = null,
                modifier = Modifier.size(VdvoemSizes.iconSm)
            )
            Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
            Text(
                text = if (pair == null) stringResource(R.string.dashboard_setup_pair)
                else stringResource(R.string.dashboard_edit_pair)
            )
        }
    }
}

@Composable
private fun UpcomingEventCard(event: CalendarEvent, onClick: () -> Unit) {
    val date = Dates.nextOccurrence(event.dateMillis, event.annual)
    val days = Dates.daysUntil(date)

    VdvoemCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.size(VdvoemSizes.avatarLg)
            ) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(VdvoemSpacing.lg)
                )
            }
            Spacer(modifier = Modifier.size(VdvoemSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = Dates.format(date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (days >= 0) {
                InfoPill(
                    text = pluralStringResource(R.plurals.dashboard_days_short, days.toInt(), days)
                )
            }
        }
    }
}

@Composable
private fun ModuleQuickRow(
    module: AppModule,
    onClick: () -> Unit
) {
    VdvoemCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(VdvoemSizes.avatarMd)
            ) {
                Icon(
                    imageVector = module.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(VdvoemSpacing.sm + VdvoemSpacing.xxs)
                )
            }
            Spacer(modifier = Modifier.size(VdvoemSpacing.md))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.xxs)
            ) {
                Text(
                    text = stringResource(module.manifest.titleRes),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(module.manifest.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PairDialog(
    pair: Pair?,
    onDismiss: () -> Unit,
    onConfirm: (nameA: String, nameB: String, sinceMillis: Long) -> Unit
) {
    var nameA by remember { mutableStateOf(pair?.nameA.orEmpty()) }
    var nameB by remember { mutableStateOf(pair?.nameB.orEmpty()) }
    var sinceMillis by remember {
        mutableLongStateOf(pair?.sinceMillis ?: Dates.startOfDay(Dates.today()))
    }
    var showPicker by remember { mutableStateOf(false) }
    val canSave = nameA.isNotBlank() && nameB.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pair_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.md)) {
                OutlinedTextField(
                    value = nameA,
                    onValueChange = { nameA = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.pair_field_you)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nameB,
                    onValueChange = { nameB = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.pair_field_partner)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { showPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(
                            R.string.pair_since_value,
                            Dates.format(Dates.toLocalDate(sinceMillis))
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(nameA, nameB, sinceMillis) },
                enabled = canSave
            ) {
                Text(stringResource(R.string.pair_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.pair_cancel))
            }
        }
    )

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = Dates.toPickerMillis(sinceMillis)
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let {
                            sinceMillis = Dates.fromPickerMillis(it)
                        }
                        showPicker = false
                    }
                ) {
                    Text(stringResource(R.string.pair_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.pair_cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
