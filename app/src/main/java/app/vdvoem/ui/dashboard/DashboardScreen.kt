package app.vdvoem.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.vdvoem.R
import app.vdvoem.core.domain.model.CalendarEvent
import app.vdvoem.core.domain.model.Pair
import app.vdvoem.core.domain.module.AppModule
import app.vdvoem.core.domain.time.Dates
import app.vdvoem.core.ui.components.InfoPill
import app.vdvoem.core.ui.components.SectionHeader
import app.vdvoem.core.ui.components.VdvoemCard
import app.vdvoem.core.ui.theme.VdvoemSizes
import app.vdvoem.core.ui.theme.VdvoemSpacing
import app.vdvoem.data.HomePreferences
import app.vdvoem.ui.presents.PresentsModule

@Composable
fun DashboardScreen(
    modules: List<AppModule>,
    pair: Pair?,
    nextEvent: CalendarEvent?,
    eventCount: Int,
    wishCount: Int,
    giftCount: Int,
    widgetIds: List<String>,
    lastPoke: String?,
    onWidgetIdsChange: (List<String>) -> Unit,
    onSendPoke: (String) -> Unit,
    onOpenModule: (AppModule) -> Unit,
    onOpenStore: () -> Unit,
    onOpenWishes: () -> Unit,
    onOpenGifts: () -> Unit,
    onOpenQuizzes: () -> Unit,
    onOpenPairEditor: () -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPokeDialog by rememberSaveable { mutableStateOf(false) }
    var showWidgetPicker by rememberSaveable { mutableStateOf(false) }
    val enabledRoutes = remember(modules) { modules.map { it.route }.toSet() }
    val visibleWidgetIds = remember(widgetIds, enabledRoutes) {
        widgetIds.filter { widgetAvailable(it, enabledRoutes) }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = VdvoemSpacing.screenHorizontal,
            end = VdvoemSpacing.screenHorizontal,
            top = VdvoemSpacing.sm,
            bottom = VdvoemSpacing.listBottom
        ),
        verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.md)
    ) {
        item(key = "pair") {
            PairCard(
                pair = pair,
                lastPoke = lastPoke,
                onEditPair = onOpenPairEditor,
                onPoke = { showPokeDialog = true }
            )
        }

        item(key = "widgets_header") {
            SectionHeader(
                title = stringResource(R.string.dashboard_widgets_title),
                modifier = Modifier.padding(top = VdvoemSpacing.sm),
                trailing = {
                    IconButton(onClick = { showWidgetPicker = true }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.dashboard_widget_add)
                        )
                    }
                }
            )
        }

        item(key = "widgets") {
            WidgetGrid(
                widgetIds = visibleWidgetIds,
                nextEvent = nextEvent,
                eventCount = eventCount,
                wishCount = wishCount,
                giftCount = giftCount,
                enabledModules = modules.size,
                onOpenCalendar = onOpenCalendar,
                onOpenWishes = onOpenWishes,
                onOpenGifts = onOpenGifts,
                onOpenQuizzes = onOpenQuizzes,
                onOpenModule = { module -> onOpenModule(module) },
                onOpenStore = onOpenStore,
                onAddWidget = { showWidgetPicker = true }
            )
        }
    }

    if (showWidgetPicker) {
        WidgetPickerSheet(
            selected = widgetIds,
            onToggle = { id, enabled ->
                val next = if (enabled) widgetIds + id else widgetIds - id
                onWidgetIdsChange(next)
            },
            onDismiss = { showWidgetPicker = false }
        )
    }

    if (showPokeDialog) {
        PokeDialog(
            onDismiss = { showPokeDialog = false },
            onSend = { text ->
                onSendPoke(text)
                showPokeDialog = false
            }
        )
    }
}

/** Большая карточка пары: аватары A и B с сердечком между ними и счётчик дней. */
@Composable
private fun PairCard(
    pair: Pair?,
    lastPoke: String?,
    onEditPair: () -> Unit,
    onPoke: () -> Unit
) {
    VdvoemCard(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        onClick = onEditPair
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            AvatarColumn(
                letter = pair?.nameA?.firstOrNull()?.uppercase() ?: "?",
                name = pair?.nameA ?: stringResource(R.string.pair_field_you)
            )
            IconButton(
                onClick = onPoke,
                modifier = Modifier.padding(horizontal = VdvoemSpacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = stringResource(R.string.dashboard_poke),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(VdvoemSizes.iconLg)
                )
            }
            AvatarColumn(
                letter = pair?.nameB?.firstOrNull()?.uppercase() ?: "?",
                name = pair?.nameB ?: stringResource(R.string.pair_field_partner)
            )
        }

        Spacer(modifier = Modifier.size(VdvoemSpacing.lg))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (pair == null) {
                Text(
                    text = stringResource(R.string.dashboard_hero_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.size(VdvoemSpacing.xs))
                Text(
                    text = stringResource(R.string.dashboard_hero_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            } else {
                val days = Dates.daysUntil(Dates.today(), Dates.toLocalDate(pair.sinceMillis))
                Text(
                    text = days.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = pluralStringResource(R.plurals.dashboard_days_word, days.toInt()),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
                Text(
                    text = stringResource(
                        R.string.dashboard_pair_since,
                        Dates.format(Dates.toLocalDate(pair.sinceMillis))
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                pair.birthdayA?.let { birthday ->
                    BirthdayLine(
                        name = pair.nameA,
                        birthday = birthday,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                pair.birthdayB?.let { birthday ->
                    BirthdayLine(
                        name = pair.nameB,
                        birthday = birthday,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                if (!lastPoke.isNullOrBlank()) {
                    Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
                    Text(
                        text = stringResource(R.string.dashboard_poke_last, lastPoke),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (pair == null) {
            Spacer(modifier = Modifier.size(VdvoemSpacing.lg))
            FilledTonalButton(
                onClick = onEditPair,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    contentColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(VdvoemSizes.iconSm)
                )
                Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
                Text(stringResource(R.string.dashboard_setup_pair))
            }
        }
    }
}

@Composable
private fun BirthdayLine(name: String, birthday: Long, color: Color) {
    val date = Dates.toLocalDate(birthday)
    Text(
        text = stringResource(R.string.dashboard_birthday, name, Dates.format(date)),
        style = MaterialTheme.typography.bodySmall,
        color = color
    )
}

@Composable
private fun AvatarColumn(letter: String, name: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(VdvoemSizes.avatarLg)
                .background(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.14f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = letter,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.size(VdvoemSpacing.xs))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun QuickCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    container: Color,
    onContainer: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    VdvoemCard(
        containerColor = container,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = onContainer,
            modifier = Modifier.size(VdvoemSizes.iconLg)
        )
        Spacer(modifier = Modifier.size(VdvoemSpacing.md))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = onContainer
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = onContainer.copy(alpha = 0.8f)
        )
    }
}

/** Сетка виджетов 2 колонки: состав задаёт пользователь, порядок — порядок в списке. */
@Composable
private fun WidgetGrid(
    widgetIds: List<String>,
    nextEvent: CalendarEvent?,
    eventCount: Int,
    wishCount: Int,
    giftCount: Int,
    enabledModules: Int,
    onOpenCalendar: () -> Unit,
    onOpenWishes: () -> Unit,
    onOpenGifts: () -> Unit,
    onOpenQuizzes: () -> Unit,
    onOpenModule: (AppModule) -> Unit,
    onOpenStore: () -> Unit,
    onAddWidget: () -> Unit
) {
    val daysToEvent = nextEvent?.let {
        Dates.daysUntil(Dates.nextOccurrence(it.dateMillis, it.annual))
    }
    val tiles: List<String?> = widgetIds.map { it as String? } + listOf(null)

    Column(verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.md)) {
        tiles.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.md)
            ) {
                row.forEach { id ->
                    if (id == null) {
                        WidgetCard(
                            icon = Icons.Filled.Add,
                            title = stringResource(R.string.dashboard_widget_add),
                            titleLines = 2,
                            value = "",
                            onClick = onAddWidget,
                            dashed = true,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        when (id) {
                            HomePreferences.WIDGET_COUNTDOWN -> WidgetCard(
                                icon = Icons.Filled.DateRange,
                                title = nextEvent?.title
                                    ?: stringResource(R.string.widget_countdown_title),
                                value = daysToEvent?.let {
                                    pluralStringResource(R.plurals.dashboard_days_short, it.toInt(), it)
                                } ?: stringResource(R.string.dashboard_widget_empty),
                                onClick = onOpenCalendar,
                                highlight = true,
                                modifier = Modifier.weight(1f)
                            )

                            HomePreferences.WIDGET_EVENT -> WidgetCard(
                                icon = Icons.Filled.DateRange,
                                title = stringResource(R.string.dashboard_upcoming_title),
                                value = pluralStringResource(
                                    R.plurals.dashboard_events_count,
                                    eventCount,
                                    eventCount
                                ),
                                onClick = onOpenCalendar,
                                modifier = Modifier.weight(1f)
                            )

                            HomePreferences.WIDGET_WISHES -> WidgetCard(
                                icon = Icons.Filled.Favorite,
                                title = stringResource(R.string.dashboard_wishes_card),
                                value = wishCount.toString(),
                                onClick = onOpenWishes,
                                modifier = Modifier.weight(1f)
                            )

                            HomePreferences.WIDGET_GIFTS -> WidgetCard(
                                icon = Icons.Filled.ShoppingCart,
                                title = stringResource(R.string.dashboard_gifts_card),
                                value = giftCount.toString(),
                                onClick = onOpenGifts,
                                modifier = Modifier.weight(1f)
                            )

                            HomePreferences.WIDGET_TESTS -> WidgetCard(
                                icon = Icons.Filled.Star,
                                title = stringResource(R.string.dashboard_tests_card),
                                value = stringResource(R.string.dashboard_tests_card_subtitle),
                                onClick = onOpenQuizzes,
                                modifier = Modifier.weight(1f)
                            )

                            HomePreferences.WIDGET_MODULES -> WidgetCard(
                                icon = Icons.Filled.Star,
                                title = stringResource(R.string.dashboard_modules_title),
                                value = enabledModules.toString(),
                                onClick = onOpenStore,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Плитка виджета. Ширину задаёт сетка (вес колонки), высота фиксирована,
 * чтобы ряды не «прыгали» при разном объёме текста.
 */
@Composable
private fun WidgetCard(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleLines: Int = 1,
    dashed: Boolean = false,
    highlight: Boolean = false
) {
    VdvoemCard(
        containerColor = when {
            highlight -> MaterialTheme.colorScheme.tertiaryContainer
            dashed -> MaterialTheme.colorScheme.surfaceContainerLow
            else -> MaterialTheme.colorScheme.surfaceContainer
        },
        modifier = modifier
            .height(VdvoemSizes.widgetHeight)
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (highlight) {
                MaterialTheme.colorScheme.onTertiaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(VdvoemSizes.iconMd)
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = if (highlight) {
                MaterialTheme.colorScheme.onTertiaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = titleLines,
            overflow = TextOverflow.Ellipsis
        )
        if (value.isNotEmpty()) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (highlight) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

/** Сопоставление виджета с модулем: виджеты выключенных модулей не показываем. */
private fun widgetAvailable(id: String, enabledRoutes: Set<String>): Boolean = when (id) {
    HomePreferences.WIDGET_EVENT -> "calendar" in enabledRoutes
    HomePreferences.WIDGET_WISHES, HomePreferences.WIDGET_GIFTS ->
        PresentsModule.ROUTE in enabledRoutes
    HomePreferences.WIDGET_TESTS -> "quizzes" in enabledRoutes
    else -> true
}

private data class WidgetEntry(
    val id: String,
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector
)

private val widgetCatalog = listOf(
    WidgetEntry(
        HomePreferences.WIDGET_COUNTDOWN,
        R.string.widget_countdown_title,
        R.string.widget_countdown_description,
        Icons.Filled.DateRange
    ),
    WidgetEntry(
        HomePreferences.WIDGET_EVENT,
        R.string.widget_event_title,
        R.string.widget_event_description,
        Icons.Filled.DateRange
    ),
    WidgetEntry(
        HomePreferences.WIDGET_WISHES,
        R.string.dashboard_wishes_card,
        R.string.widget_wishes_description,
        Icons.Filled.Favorite
    ),
    WidgetEntry(
        HomePreferences.WIDGET_GIFTS,
        R.string.dashboard_gifts_card,
        R.string.widget_gifts_description,
        Icons.Filled.ShoppingCart
    ),
    WidgetEntry(
        HomePreferences.WIDGET_TESTS,
        R.string.dashboard_tests_card,
        R.string.widget_tests_description,
        Icons.Filled.Star
    ),
    WidgetEntry(
        HomePreferences.WIDGET_MODULES,
        R.string.dashboard_modules_title,
        R.string.widget_modules_description,
        Icons.Filled.Star
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetPickerSheet(
    selected: List<String>,
    onToggle: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VdvoemSpacing.screenHorizontal)
                .padding(bottom = VdvoemSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
        ) {
            Text(
                text = stringResource(R.string.widget_picker_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.widget_picker_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            widgetCatalog.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = entry.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(VdvoemSizes.iconMd)
                    )
                    Spacer(modifier = Modifier.width(VdvoemSpacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(entry.titleRes),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(entry.descriptionRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = entry.id in selected,
                        onCheckedChange = { onToggle(entry.id, it) }
                    )
                }
            }
            Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
            FilledTonalButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.widget_picker_done))
            }
        }
    }
}

@Composable
private fun PokeDialog(
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val quickMessages = listOf(
        stringResource(R.string.poke_quick_love),
        stringResource(R.string.poke_quick_miss),
        stringResource(R.string.poke_quick_hug),
        stringResource(R.string.poke_quick_thinking)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.poke_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)) {
                Text(
                    text = stringResource(R.string.poke_dialog_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                quickMessages.forEach { message ->
                    OutlinedButton(
                        onClick = { text = message },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(message)
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.poke_field)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSend(text.trim()) },
                enabled = text.isNotBlank()
            ) {
                Text(stringResource(R.string.poke_send))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.pair_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairDialog(
    pair: Pair?,
    onDismiss: () -> Unit,
    onConfirm: (nameA: String, nameB: String, sinceMillis: Long, birthdayA: Long?, birthdayB: Long?) -> Unit
) {
    var nameA by remember { mutableStateOf(pair?.nameA.orEmpty()) }
    var nameB by remember { mutableStateOf(pair?.nameB.orEmpty()) }
    var sinceMillis by remember {
        mutableLongStateOf(pair?.sinceMillis ?: Dates.startOfDay(Dates.today()))
    }
    var birthdayA by remember { mutableStateOf(pair?.birthdayA) }
    var birthdayB by remember { mutableStateOf(pair?.birthdayB) }
    var pickerTarget by remember { mutableStateOf<Int?>(null) }
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
                    onClick = { pickerTarget = 0 },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(
                            R.string.pair_since_value,
                            Dates.format(Dates.toLocalDate(sinceMillis))
                        )
                    )
                }
                OutlinedButton(
                    onClick = { pickerTarget = 1 },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = birthdayA?.let {
                            stringResource(R.string.pair_birthday_a_value, Dates.format(Dates.toLocalDate(it)))
                        } ?: stringResource(R.string.pair_birthday_a)
                    )
                }
                OutlinedButton(
                    onClick = { pickerTarget = 2 },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = birthdayB?.let {
                            stringResource(R.string.pair_birthday_b_value, Dates.format(Dates.toLocalDate(it)))
                        } ?: stringResource(R.string.pair_birthday_b)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(nameA, nameB, sinceMillis, birthdayA, birthdayB) },
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

    val picker = pickerTarget ?: return
    val initial = when (picker) {
        1 -> birthdayA
        2 -> birthdayB
        else -> sinceMillis
    }
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = Dates.toPickerMillis(
            initial ?: Dates.startOfDay(Dates.today())
        )
    )
    DatePickerDialog(
        onDismissRequest = { pickerTarget = null },
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { picked ->
                        val value = Dates.fromPickerMillis(picked)
                        when (picker) {
                            1 -> birthdayA = value
                            2 -> birthdayB = value
                            else -> sinceMillis = value
                        }
                    }
                    pickerTarget = null
                }
            ) {
                Text(stringResource(R.string.pair_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = { pickerTarget = null }) {
                Text(stringResource(R.string.pair_cancel))
            }
        }
    ) {
        DatePicker(state = pickerState)
    }
}
