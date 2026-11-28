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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.vdvoem.R
import app.vdvoem.core.domain.model.CalendarEvent
import app.vdvoem.core.domain.model.DiaryEntry
import app.vdvoem.core.domain.model.DiaryMood
import app.vdvoem.core.domain.model.DiaryReminder
import app.vdvoem.core.domain.model.Lesson
import app.vdvoem.core.domain.model.Pair
import app.vdvoem.core.domain.model.Poke
import app.vdvoem.core.domain.module.AppModule
import app.vdvoem.core.domain.time.Dates
import java.time.LocalDate
import app.vdvoem.core.ui.components.InfoPill
import app.vdvoem.core.ui.components.SectionHeader
import app.vdvoem.core.ui.components.VdvoemCard
import app.vdvoem.core.ui.theme.VdvoemSizes
import app.vdvoem.core.ui.theme.VdvoemSpacing
import app.vdvoem.core.domain.cycle.CyclePhase
import app.vdvoem.core.domain.cycle.CycleStats
import app.vdvoem.data.CountdownConfig
import app.vdvoem.data.HomePreferences
import app.vdvoem.data.HomeWidget
import kotlin.math.absoluteValue
import app.vdvoem.ui.presents.PresentsModule
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DashboardScreen(
    modules: List<AppModule>,
    pair: Pair?,
    events: List<CalendarEvent>,
    eventCount: Int,
    wishCount: Int,
    giftCount: Int,
    widgets: List<HomeWidget>,
    lastPoke: String?,
    pokes: List<Poke> = emptyList(),
    sentPokeIds: Set<String> = emptySet(),
    cycleStats: CycleStats? = null,
    refreshing: Boolean = false,
    /** Синхронизация настроена и включена: подсказки «увидит после синхронизации» не нужны. */
    syncReady: Boolean = true,
    /** Сегодняшняя дата: приходит из состояния, чтобы шторка и уроки жили по одному дню. */
    today: LocalDate,
    /** Уроки сегодняшнего дня — для шторки «Сегодня». */
    todayLessons: List<Lesson> = emptyList(),
    /** Напоминания дневника на сегодня. */
    todayReminders: List<DiaryReminder> = emptyList(),
    /** Запись дневника за сегодня — показываем её в шторке «Сегодня». */
    todayEntry: DiaryEntry? = null,
    /** Ближайшие события в пределах срока напоминаний. */
    upcomingEvents: List<TodayEvent> = emptyList(),
    onRefresh: () -> Unit = {},
    onWidgetsChange: (List<HomeWidget>) -> Unit,
    onAddWidget: (String) -> Unit,
    onSendPoke: (String) -> Unit,
    onOpenModule: (AppModule) -> Unit,
    onOpenStore: () -> Unit,
    onOpenWishes: () -> Unit,
    onOpenGifts: () -> Unit,
    onOpenQuizzes: () -> Unit,
    onOpenCycle: () -> Unit,
    onOpenPairEditor: () -> Unit,
    onOpenCalendar: () -> Unit,
    onReminderDone: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showPokeDialog by rememberSaveable { mutableStateOf(false) }
    var showPokeHistory by rememberSaveable { mutableStateOf(false) }
    var showToday by rememberSaveable { mutableStateOf(false) }
    var showWidgetPicker by rememberSaveable { mutableStateOf(false) }
    var configWidgetId by rememberSaveable { mutableStateOf<String?>(null) }
    val enabledRoutes = remember(modules) { modules.map { it.route }.toSet() }
    // Блоки шторки повторяют навигацию: нет модуля — нет и раздела, а не «уроков нет».
    val lessonsAvailable = "timetable" in enabledRoutes
    val eventsAvailable = "calendar" in enabledRoutes
    val remindersAvailable = "diary" in enabledRoutes
    val todayLessonsVisible = remember(todayLessons, lessonsAvailable) {
        if (lessonsAvailable) todayLessons else emptyList()
    }
    val visibleEvents = remember(upcomingEvents, eventsAvailable) {
        if (eventsAvailable) upcomingEvents else emptyList()
    }
    val visibleReminders = remember(todayReminders, remindersAvailable) {
        if (remindersAvailable) todayReminders else emptyList()
    }
    val visibleDiaryEntry = if (remindersAvailable) todayEntry else null
    val visibleWidgets = remember(widgets, enabledRoutes) {
        widgets.filter { widgetAvailable(it.type, enabledRoutes) }
    }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
                onPoke = { showPokeDialog = true },
                todayLessonCount = todayLessonsVisible.size,
                hasUpcomingEvents = visibleEvents.any { !it.isBirthday },
                onOpenToday = { showToday = true }
            )
        }

        if (pokes.isNotEmpty()) {
            item(key = "poke_history") {
                TextButton(onClick = { showPokeHistory = true }) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(VdvoemSizes.iconSm)
                    )
                    Spacer(modifier = Modifier.size(VdvoemSpacing.xs))
                    Text(stringResource(R.string.poke_history_title))
                }
            }
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
                widgets = visibleWidgets,
                events = events,
                pair = pair,
                eventCount = eventCount,
                wishCount = wishCount,
                giftCount = giftCount,
                enabledModules = modules.size,
                cycleStats = cycleStats,
                todayEntry = visibleDiaryEntry,
                todayReminders = visibleReminders,
                onOpenCycle = onOpenCycle,
                onOpenPair = onOpenPairEditor,
                onOpenCalendar = onOpenCalendar,
                onOpenWishes = onOpenWishes,
                onOpenGifts = onOpenGifts,
                onOpenQuizzes = onOpenQuizzes,
                onOpenModule = { module -> onOpenModule(module) },
                onOpenStore = onOpenStore,
                onOpenDiary = {
                    modules.firstOrNull { it.route == "diary" }?.let(onOpenModule)
                    showToday = false
                },
                onAddWidget = { showWidgetPicker = true },
                onConfigure = { id -> configWidgetId = id }
            )
        }
    }
    }

    if (showWidgetPicker) {
        WidgetPickerSheet(
            widgets = widgets,
            enabledRoutes = enabledRoutes,
            events = events,
            pair = pair,
            onAdd = { type -> onAddWidget(type) },
            onChange = onWidgetsChange,
            onConfigure = { id ->
                configWidgetId = id
                showWidgetPicker = false
            },
            onDismiss = { showWidgetPicker = false }
        )
    }

    val configTarget = widgets.firstOrNull { it.id == configWidgetId }
    if (configTarget != null) {
        CountdownSettingsSheet(
            widget = configTarget,
            events = events,
            pair = pair,
            onChange = { updated ->
                onWidgetsChange(widgets.map { if (it.id == updated.id) updated else it })
            },
            onDelete = {
                onWidgetsChange(widgets.filterNot { it.id == configTarget.id })
                configWidgetId = null
            },
            onDismiss = { configWidgetId = null }
        )
    }

    if (showPokeDialog) {
        PokeDialog(
            syncReady = syncReady,
            onDismiss = { showPokeDialog = false },
            onSend = { text ->
                onSendPoke(text)
                showPokeDialog = false
            },
            onOpenHistory = {
                showPokeDialog = false
                showPokeHistory = true
            }
        )
    }

    if (showPokeHistory) {
        PokeHistorySheet(
            pokes = pokes,
            sentPokeIds = sentPokeIds,
            onDismiss = { showPokeHistory = false }
        )
    }

    if (showToday) {
        TodaySheet(
            today = today,
            lessons = todayLessonsVisible,
            lessonsAvailable = lessonsAvailable,
            events = visibleEvents,
            eventsAvailable = eventsAvailable,
            diaryEntry = visibleDiaryEntry,
            reminders = visibleReminders,
            remindersAvailable = remindersAvailable,
            onReminderDone = onReminderDone,
            onOpenDiary = {
                // Дневник открываем как обычную вкладку: шторка просто закрывается.
                modules.firstOrNull { it.route == "diary" }?.let(onOpenModule)
                showToday = false
            },
            onDismiss = { showToday = false }
        )
    }
}

/** Большая карточка пары: аватары A и B с сердечком между ними и счётчик дней. */
@Composable
private fun PairCard(
    pair: Pair?,
    lastPoke: String?,
    todayLessonCount: Int,
    hasUpcomingEvents: Boolean,
    onEditPair: () -> Unit,
    onPoke: () -> Unit,
    onOpenToday: () -> Unit
) {
    VdvoemCard(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        onClick = onEditPair
    ) {
        // Кнопка «Сегодня» — поверх правого верхнего угла карточки, рядом с аватарами.
        Box(modifier = Modifier.fillMaxWidth()) {
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
            if (pair != null) {
                TodayButton(
                    lessons = todayLessonCount,
                    hasEvents = hasUpcomingEvents,
                    onClick = onOpenToday,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
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
                // sinceMillis == 0 бывает у пары, созданной до появления даты начала:
                // считать «20 772 дня вместе с 1 января 1970» нельзя.
                val hasSince = pair.sinceMillis > 0
                if (hasSince) {
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
                } else {
                    Text(
                        text = stringResource(R.string.dashboard_pair_since_missing),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
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

/**
 * Круглая кнопка «Сегодня» в углу карточки пары.
 * Цифра — сколько сегодня уроков, точка — есть событие календаря, пусто — просто календарик.
 */
@Composable
private fun TodayButton(
    lessons: Int,
    hasEvents: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Экранный диктор должен знать и про уроки, и про точку: она декоративная.
    val description = buildString {
        append(stringResource(R.string.dashboard_today))
        if (lessons > 0) {
            append(", ")
            append(stringResource(R.string.dashboard_today_cd_lessons, lessons))
        }
        if (hasEvents) {
            append(", ")
            append(stringResource(R.string.dashboard_today_cd_events))
        }
    }
    Box(modifier = modifier.size(VdvoemSizes.touchTarget)) {
        IconButton(onClick = onClick) {
            Box(
                modifier = Modifier
                    .size(VdvoemSizes.roundButton)
                    .background(
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = description,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(VdvoemSizes.iconMd)
                )
            }
        }
        // Счётчик уроков и точка «есть событие» живут независимо: точка не должна
        // пропадать в день с уроками. Оба значка — отдельным слоем поверх кнопки.
        if (lessons > 0) {
            Badge(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = VdvoemSpacing.xxs, y = -VdvoemSpacing.xxs)
                    .clearAndSetSemantics { }
            ) {
                Text(text = lessons.toString(), style = MaterialTheme.typography.labelSmall)
            }
        }
        if (hasEvents) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = -VdvoemSpacing.xs, y = -VdvoemSpacing.xs)
                    .size(VdvoemSizes.dot)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

/** Шторка «Сегодня»: уроки, ближайшие события и напоминания дневника. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodaySheet(
    today: LocalDate,
    lessons: List<Lesson>,
    lessonsAvailable: Boolean,
    events: List<TodayEvent>,
    eventsAvailable: Boolean,
    diaryEntry: DiaryEntry?,
    reminders: List<DiaryReminder>,
    remindersAvailable: Boolean,
    onReminderDone: (String) -> Unit,
    onOpenDiary: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = VdvoemSizes.sheetMax)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VdvoemSpacing.screenHorizontal)
                .padding(bottom = VdvoemSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
        ) {
            Text(
                text = stringResource(R.string.dashboard_today),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = Dates.formatWeekday(today),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.size(VdvoemSpacing.xs))

            var hasSection = false

            // Раздел показываем только вместе с его модулем: иначе «уроков нет» врёт,
            // когда уроки просто спрятаны вместе с «Расписанием».
            if (lessonsAvailable) {
                Text(
                    text = stringResource(R.string.dashboard_today_lessons),
                    style = MaterialTheme.typography.titleMedium
                )
                if (lessons.isEmpty()) {
                    Text(
                        text = stringResource(R.string.dashboard_today_lessons_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    lessons.forEach { lesson -> TodayLessonRow(lesson) }
                }
                hasSection = true
            }

            if (eventsAvailable) {
                if (hasSection) HorizontalDivider()
                Text(
                    text = stringResource(R.string.dashboard_today_events),
                    style = MaterialTheme.typography.titleMedium
                )
                if (events.isEmpty()) {
                    Text(
                        text = stringResource(R.string.dashboard_today_events_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    events.forEach { event -> TodayEventRow(event) }
                }
                hasSection = true
            }

            if (remindersAvailable) {
                if (hasSection) HorizontalDivider()
                Text(
                    text = stringResource(R.string.dashboard_today_diary),
                    style = MaterialTheme.typography.titleMedium
                )
                if (diaryEntry == null) {
                    Text(
                        text = stringResource(R.string.dashboard_today_diary_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    TodayDiaryEntry(diaryEntry)
                }
                TextButton(onClick = onOpenDiary) {
                    Text(stringResource(R.string.dashboard_today_diary_open))
                }
                hasSection = true
            }

            if (remindersAvailable) {
                if (hasSection) HorizontalDivider()
                Text(
                    text = stringResource(R.string.dashboard_today_reminders),
                    style = MaterialTheme.typography.titleMedium
                )
                if (reminders.isEmpty()) {
                    Text(
                        text = stringResource(R.string.dashboard_today_reminders_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    reminders.forEach { reminder ->
                        TodayReminderRow(
                            reminder = reminder,
                            onDone = { onReminderDone(reminder.id) }
                        )
                    }
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.connect_done))
            }
        }
    }
}

/** Сегодняшняя запись дневника: заголовок и несколько строк текста. */
@Composable
private fun TodayDiaryEntry(entry: DiaryEntry) {
    val title = entry.title.trim().ifBlank {
        entry.body.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
    }
    val rest = entry.body.trim()
        .takeIf { it.isNotEmpty() && it != title }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = VdvoemSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.xs)
    ) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        rest?.let { body ->
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Событие и сколько до него осталось: «сегодня», «завтра» или «через N дней». */
@Composable
private fun TodayEventRow(event: TodayEvent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = VdvoemSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
    ) {
        Icon(
            imageVector = if (event.isBirthday) Icons.Filled.Favorite else Icons.Filled.DateRange,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(VdvoemSizes.iconSm)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (event.isBirthday) {
                    stringResource(R.string.dashboard_today_birthday, event.title)
                } else {
                    event.title
                },
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = eventCountdownText(event.daysUntil),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun eventCountdownText(daysUntil: Long): String {
    val days = daysUntil.toInt()
    return when (days) {
        0 -> stringResource(R.string.dashboard_today_event_today)
        1 -> stringResource(R.string.dashboard_today_event_tomorrow)
        else -> pluralStringResource(R.plurals.dashboard_today_event_in_days, days, days)
    }
}

/** Одна строка урока: время, предмет, кабинет и кто берёт учебник. */
@Composable
private fun TodayLessonRow(lesson: Lesson) {
    val details = buildList {
        if (lesson.room.isNotBlank()) add(stringResource(R.string.dashboard_today_room, lesson.room))
        if (lesson.textbookBy.isNotBlank()) add(stringResource(R.string.dashboard_today_textbook, lesson.textbookBy))
    }.joinToString(" · ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = VdvoemSpacing.xs),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
    ) {
        Text(
            text = lesson.timeRange,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(min = VdvoemSizes.timeColumn)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = lesson.subject,
                style = MaterialTheme.typography.bodyLarge
            )
            if (details.isNotEmpty()) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Время напоминания в шторке: только часы и минуты, день и так сегодняшний. */
private val reminderClockFormat: DateTimeFormatter
    get() = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

/** Одна строка напоминания: время, текст и кнопка «выполнено». */
@Composable
private fun TodayReminderRow(
    reminder: DiaryReminder,
    onDone: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = VdvoemSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
    ) {
        Text(
            text = Instant.ofEpochMilli(reminder.remindAt)
                .atZone(ZoneId.systemDefault())
                .format(reminderClockFormat),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.widthIn(min = VdvoemSizes.timeColumn)
        )
        Text(
            text = reminder.text,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDone) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = stringResource(R.string.dashboard_today_reminder_done),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(VdvoemSizes.iconMd)
            )
        }
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
    widgets: List<HomeWidget>,
    events: List<CalendarEvent>,
    pair: Pair?,
    eventCount: Int,
    wishCount: Int,
    giftCount: Int,
    enabledModules: Int,
    cycleStats: CycleStats?,
    todayEntry: DiaryEntry?,
    todayReminders: List<DiaryReminder>,
    onOpenCycle: () -> Unit,
    onOpenPair: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenWishes: () -> Unit,
    onOpenGifts: () -> Unit,
    onOpenQuizzes: () -> Unit,
    onOpenModule: (AppModule) -> Unit,
    onOpenStore: () -> Unit,
    onOpenDiary: () -> Unit,
    onAddWidget: () -> Unit,
    onConfigure: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.md)) {
        widgets.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.md)
            ) {
                row.forEach { widget ->
                    when (widget.type) {
                        HomePreferences.WIDGET_COUNTDOWN -> {
                            val result = resolveCountdown(widget.config, events, pair)
                            val autoTitle = when (widget.config.target) {
                                CountdownConfig.TARGET_PAIR ->
                                    stringResource(R.string.countdown_target_pair)
                                CountdownConfig.TARGET_BIRTHDAY_A ->
                                    if (pair != null) stringResource(R.string.countdown_target_birthday, pair.nameA) else null
                                CountdownConfig.TARGET_BIRTHDAY_B ->
                                    if (pair != null) stringResource(R.string.countdown_target_birthday, pair.nameB) else null
                                CountdownConfig.TARGET_EVENT ->
                                    events.firstOrNull { it.id == widget.config.eventId }?.title
                                else -> null
                            }
                            val title = widget.config.title
                                ?: result?.eventTitle
                                ?: autoTitle
                                ?: stringResource(R.string.widget_countdown_title)
                            val days = result?.days
                            WidgetCard(
                                icon = Icons.Filled.DateRange,
                                title = title,
                                value = when {
                                    days == null -> stringResource(R.string.dashboard_widget_empty)
                                    days == 0L -> stringResource(R.string.dashboard_today)
                                    widget.config.isSince -> pluralStringResource(
                                        R.plurals.dashboard_days_short,
                                        days.absoluteValue.toInt(),
                                        days.absoluteValue
                                    )
                                    days < 0L -> pluralStringResource(
                                        R.plurals.widget_days_passed,
                                        (-days).toInt(),
                                        -days
                                    )
                                    else -> pluralStringResource(
                                        R.plurals.dashboard_days_short, days.toInt(), days
                                    )
                                },
                                onClick = { onConfigure(widget.id) },
                                caption = result?.let { countdown ->
                                    if (widget.config.isSince) {
                                        stringResource(
                                            R.string.widget_countdown_since_date,
                                            Dates.format(countdown.date)
                                        )
                                    } else {
                                        Dates.format(countdown.date)
                                    }
                                },
                                highlight = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        HomePreferences.WIDGET_EVENT -> WidgetCard(
                            icon = Icons.Filled.DateRange,
                            title = stringResource(R.string.dashboard_upcoming_title),
                            value = pluralStringResource(R.plurals.dashboard_events_count, eventCount, eventCount),
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
                        HomePreferences.WIDGET_TOGETHER -> {
                            val since = pair?.sinceMillis ?: 0L
                            val days = if (since > 0L) {
                                Dates.daysUntil(Dates.today(), Dates.toLocalDate(since))
                            } else {
                                0L
                            }
                            WidgetCard(
                                icon = Icons.Filled.Favorite,
                                title = stringResource(R.string.widget_together_title),
                                value = if (since > 0L) {
                                    pluralStringResource(
                                        R.plurals.dashboard_days_word,
                                        days.toInt().coerceAtLeast(0),
                                        days.toInt().coerceAtLeast(0)
                                    )
                                } else {
                                    stringResource(R.string.widget_together_empty)
                                },
                                onClick = onOpenPair,
                                caption = if (since > 0L) {
                                    stringResource(
                                        R.string.widget_together_since,
                                        Dates.format(Dates.toLocalDate(since))
                                    )
                                } else {
                                    null
                                },
                                highlight = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        HomePreferences.WIDGET_CYCLE -> {
                            val info = cycleStats?.takeIf { it.lastPeriodStart != null }
                            val caption = if (info == null) null else {
                                val until = info.daysUntilNextPeriod ?: 0L
                                when {
                                    info.isLate -> stringResource(
                                        R.string.widget_cycle_late,
                                        (-until).toInt().coerceAtLeast(1)
                                    )
                                    until <= 0L -> stringResource(R.string.widget_cycle_period_now)
                                    else -> stringResource(
                                        R.string.widget_cycle_until_short,
                                        until.toInt()
                                    )
                                }
                            }
                            WidgetCard(
                                icon = Icons.Filled.Favorite,
                                title = stringResource(R.string.widget_cycle_title),
                                value = when {
                                    info == null -> stringResource(R.string.widget_cycle_no_data)
                                    else -> stringResource(R.string.widget_cycle_day, info.currentDay)
                                },
                                onClick = onOpenCycle,
                                caption = caption,
                                highlight = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        HomePreferences.WIDGET_MODULES -> WidgetCard(
                            icon = Icons.Filled.Star,
                            title = stringResource(R.string.dashboard_modules_title),
                            value = enabledModules.toString(),
                            onClick = onOpenStore,
                            modifier = Modifier.weight(1f)
                        )
                        HomePreferences.WIDGET_DIARY -> {
                            val entryTitle = todayEntry?.title?.takeIf { it.isNotBlank() }
                                ?: todayEntry?.body?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim()
                                ?: stringResource(R.string.widget_diary_empty)
                            val moodIcon = when (todayEntry?.mood) {
                                DiaryMood.JOY -> Icons.Filled.Favorite
                                DiaryMood.SAD, DiaryMood.TIRED -> Icons.Filled.Star
                                else -> Icons.Filled.DateRange
                            }
                            WidgetCard(
                                icon = moodIcon,
                                title = stringResource(R.string.widget_diary_label),
                                value = entryTitle,
                                onClick = onOpenDiary,
                                caption = todayReminders.firstOrNull()?.let { r ->
                                    val time = Instant.ofEpochMilli(r.remindAt)
                                        .atZone(ZoneId.systemDefault())
                                        .format(reminderClockFormat)
                                    stringResource(R.string.widget_diary_reminder, time, r.text)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.md)) {
            WidgetCard(
                icon = Icons.Filled.Add,
                title = stringResource(R.string.dashboard_widget_add),
                titleLines = 2,
                value = "",
                onClick = onAddWidget,
                dashed = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.weight(1f))
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
    caption: String? = null,
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

        if (!caption.isNullOrBlank()) {
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (highlight) {
                    MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.75f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

private fun widgetAvailable(type: String, enabledRoutes: Set<String>): Boolean = when (type) {
    HomePreferences.WIDGET_EVENT -> "calendar" in enabledRoutes
    HomePreferences.WIDGET_WISHES, HomePreferences.WIDGET_GIFTS ->
        PresentsModule.ROUTE in enabledRoutes
    HomePreferences.WIDGET_TESTS -> "quizzes" in enabledRoutes
    HomePreferences.WIDGET_CYCLE -> "cycle" in enabledRoutes
    HomePreferences.WIDGET_DIARY -> "diary" in enabledRoutes
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
        HomePreferences.WIDGET_CYCLE,
        R.string.widget_cycle_title,
        R.string.widget_cycle_description,
        Icons.Filled.Favorite
    ),
    WidgetEntry(
        HomePreferences.WIDGET_MODULES,
        R.string.dashboard_modules_title,
        R.string.widget_modules_description,
        Icons.Filled.Star
    ),
    WidgetEntry(
        HomePreferences.WIDGET_DIARY,
        R.string.widget_diary_label,
        R.string.widget_diary_description,
        Icons.Filled.DateRange
    ),
    WidgetEntry(
        HomePreferences.WIDGET_TOGETHER,
        R.string.widget_together_title,
        R.string.widget_together_description,
        Icons.Filled.Favorite
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetPickerSheet(
    widgets: List<HomeWidget>,
    enabledRoutes: Set<String>,
    events: List<CalendarEvent>,
    pair: Pair?,
    onAdd: (String) -> Unit,
    onChange: (List<HomeWidget>) -> Unit,
    onConfigure: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
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
            widgets.forEachIndexed { index, widget ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.width(VdvoemSizes.iconLg)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = widgetLabel(widget, events, pair),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = widgetDescription(widget.type),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { onChange(widgets.toMutableList().apply { add(index - 1, removeAt(index)) }) },
                        enabled = index > 0
                    ) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.widget_move_up)) }
                    IconButton(
                        onClick = { onChange(widgets.toMutableList().apply { add(index + 1, removeAt(index)) }) },
                        enabled = index < widgets.lastIndex
                    ) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.widget_move_down)) }
                    IconButton(
                        onClick = { onConfigure(widget.id) },
                        enabled = widget.type == HomePreferences.WIDGET_COUNTDOWN
                    ) { Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.widget_configure)) }
                    IconButton(onClick = { onChange(widgets.filterNot { it.id == widget.id }) }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.widget_remove))
                    }
                }
            }
            HorizontalDivider()
            Text(stringResource(R.string.widget_add_section), style = MaterialTheme.typography.titleMedium)
            widgetCatalog.forEach { entry ->
                val available = widgetAvailable(entry.id, enabledRoutes)
                if (available) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAdd(entry.id) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(entry.icon, contentDescription = null, modifier = Modifier.size(VdvoemSizes.iconMd))
                        Spacer(modifier = Modifier.width(VdvoemSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(entry.titleRes), style = MaterialTheme.typography.bodyLarge)
                            Text(stringResource(entry.descriptionRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onAdd(entry.id) }) {
                            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.dashboard_widget_add))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.size(VdvoemSpacing.sm))
            FilledTonalButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.widget_picker_done))
            }
        }
    }
}

@Composable
private fun widgetLabel(widget: HomeWidget, events: List<CalendarEvent>, pair: Pair?): String {
    if (widget.type != HomePreferences.WIDGET_COUNTDOWN) {
        val entry = widgetCatalog.firstOrNull { it.id == widget.type }
        return entry?.let { stringResource(it.titleRes) } ?: widget.type
    }
    return widget.config.title
        ?: when (widget.config.target) {
            CountdownConfig.TARGET_EVENT -> events.firstOrNull { it.id == widget.config.eventId }?.title
            CountdownConfig.TARGET_PAIR -> stringResource(R.string.countdown_target_pair)
            CountdownConfig.TARGET_BIRTHDAY_A -> pair?.nameA?.let { stringResource(R.string.countdown_target_birthday, it) }
            CountdownConfig.TARGET_BIRTHDAY_B -> pair?.nameB?.let { stringResource(R.string.countdown_target_birthday, it) }
            else -> stringResource(R.string.widget_countdown_title)
        }
        ?: stringResource(R.string.widget_countdown_title)
}

@Composable
private fun widgetDescription(type: String): String {
    val res = widgetCatalog.firstOrNull { it.id == type }?.descriptionRes
    return if (res != null) stringResource(res) else ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountdownSettingsSheet(
    widget: HomeWidget,
    events: List<CalendarEvent>,
    pair: Pair?,
    onChange: (HomeWidget) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var config by remember(widget.id) { mutableStateOf(widget.config) }
    val customTitle = config.title.orEmpty()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = VdvoemSpacing.screenHorizontal)
                .padding(bottom = VdvoemSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
        ) {
            Text(stringResource(R.string.countdown_settings_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.countdown_target_section), style = MaterialTheme.typography.titleMedium)
            RadioOption(
                label = stringResource(R.string.countdown_target_nearest),
                selected = config.target == CountdownConfig.TARGET_NEAREST,
                onClick = { config = config.copy(target = CountdownConfig.TARGET_NEAREST, eventId = null) }
            )
            events.forEach { event ->
                RadioOption(
                    label = event.title,
                    selected = config.target == CountdownConfig.TARGET_EVENT && config.eventId == event.id,
                    onClick = { config = config.copy(target = CountdownConfig.TARGET_EVENT, eventId = event.id) }
                )
            }
            HorizontalDivider()
            Text(stringResource(R.string.countdown_special_targets), style = MaterialTheme.typography.titleMedium)
            RadioOption(
                label = stringResource(R.string.countdown_target_pair),
                selected = config.target == CountdownConfig.TARGET_PAIR,
                enabled = pair != null,
                onClick = { config = config.copy(target = CountdownConfig.TARGET_PAIR, eventId = null) }
            )
            RadioOption(
                label = pair?.let { stringResource(R.string.countdown_target_birthday, it.nameA) }
                    ?: stringResource(R.string.countdown_target_birthday, "—"),
                selected = config.target == CountdownConfig.TARGET_BIRTHDAY_A,
                enabled = pair?.birthdayA != null,
                onClick = { config = config.copy(target = CountdownConfig.TARGET_BIRTHDAY_A, eventId = null) }
            )
            RadioOption(
                label = pair?.let { stringResource(R.string.countdown_target_birthday, it.nameB) }
                    ?: stringResource(R.string.countdown_target_birthday, "—"),
                selected = config.target == CountdownConfig.TARGET_BIRTHDAY_B,
                enabled = pair?.birthdayB != null,
                onClick = { config = config.copy(target = CountdownConfig.TARGET_BIRTHDAY_B, eventId = null) }
            )
            HorizontalDivider()
            Text(stringResource(R.string.countdown_direction_section), style = MaterialTheme.typography.titleMedium)
            RadioOption(
                label = stringResource(R.string.countdown_direction_until),
                selected = !config.isSince,
                onClick = { config = config.copy(direction = CountdownConfig.DIRECTION_UNTIL) }
            )
            RadioOption(
                label = stringResource(R.string.countdown_direction_since),
                selected = config.isSince,
                onClick = { config = config.copy(direction = CountdownConfig.DIRECTION_SINCE) }
            )
            OutlinedTextField(
                value = customTitle,
                onValueChange = { config = config.copy(title = it.takeIf(String::isNotBlank)) },
                label = { Text(stringResource(R.string.countdown_custom_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            val preview = resolveCountdown(config, events, pair)
            Text(
                text = preview?.let { result ->
                    val value = when {
                        result.days == 0L -> stringResource(R.string.dashboard_today)
                        config.isSince -> pluralStringResource(
                            R.plurals.dashboard_days_short,
                            result.days.absoluteValue.toInt(),
                            result.days.absoluteValue
                        )
                        result.days < 0L -> stringResource(R.string.countdown_preview_passed)
                        else -> pluralStringResource(
                            R.plurals.dashboard_days_short,
                            result.days.toInt(),
                            result.days
                        )
                    }
                    stringResource(R.string.countdown_preview, widgetLabel(widget.copy(config = config), events, pair), value, Dates.format(result.date))
                } ?: stringResource(R.string.dashboard_widget_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilledTonalButton(
                onClick = { onChange(widget.copy(config = config)); onDismiss() },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.widget_picker_done)) }
            TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.widget_remove))
            }
        }
    }
}

@Composable
private fun RadioOption(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, enabled = enabled, onClick = onClick)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PokeDialog(
    syncReady: Boolean = true,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit,
    onOpenHistory: () -> Unit = {}
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
                // Когда пара уже синхронизируется, обещание «увидит после синхронизации»
                // только путало: сердечко уходит сразу.
                if (!syncReady) {
                    Text(
                        text = stringResource(R.string.poke_dialog_text),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                TextButton(
                    onClick = onOpenHistory,
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text(stringResource(R.string.poke_history_title))
                }
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
private fun PokeHistorySheet(
    pokes: List<Poke>,
    sentPokeIds: Set<String>,
    onDismiss: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("d MMMM, HH:mm", Locale("ru")) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VdvoemSpacing.screenHorizontal)
                .padding(bottom = VdvoemSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
        ) {
            Text(
                text = stringResource(R.string.poke_history_title),
                style = MaterialTheme.typography.titleLarge
            )
            if (pokes.isEmpty()) {
                Text(
                    text = stringResource(R.string.poke_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.xs)
                ) {
                    pokes.forEach { poke ->
                        val mine = poke.id in sentPokeIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = VdvoemSpacing.xs),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = poke.text,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "${if (mine) stringResource(R.string.poke_history_mine) else stringResource(R.string.poke_history_partner)} · " +
                                        formatter.format(Date(poke.createdAt)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(VdvoemSizes.iconSm)
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.connect_done))
            }
        }
    }
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
