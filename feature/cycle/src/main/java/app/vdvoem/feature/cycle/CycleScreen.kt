package app.vdvoem.feature.cycle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import app.vdvoem.core.domain.cycle.CyclePhase
import app.vdvoem.core.domain.model.Flow
import app.vdvoem.core.domain.time.Dates
import app.vdvoem.core.ui.components.EmptyState
import app.vdvoem.core.ui.components.InfoPill
import app.vdvoem.core.ui.components.SectionHeader
import app.vdvoem.core.ui.components.VdvoemCard
import app.vdvoem.core.ui.theme.VdvoemSizes
import app.vdvoem.core.ui.theme.VdvoemSpacing
import app.vdvoem.core.ui.theme.VdvoemTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Стабильные ключи симптомов — совпадают с доменной моделью. */
private val symptomKeys = listOf(
    "cramps", "headache", "backache", "acne", "bloating",
    "fatigue", "nausea", "breasts", "sleep"
)

/** Стабильные ключи настроения. */
private val moodKeys = listOf("happy", "calm", "sad", "irritable", "anxious", "energetic")

/**
 * Трекер цикла пары: hero-карточка, календарь месяца, история, график и советы.
 *
 * Все записи общие: второй партнёр видит их на своём телефоне после синка.
 */
@Composable
fun CycleScreen(
    state: CycleUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenPeriodSheet: () -> Unit,
    onOpenSymptoms: () -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onStartPeriodToday: () -> Unit,
    onEndPeriodToday: () -> Unit,
    onStartPeriodAt: (Long) -> Unit,
    onDismissSheet: () -> Unit,
    onUpdateDraft: ((CycleDayDraft) -> CycleDayDraft) -> Unit,
    onSaveDraft: () -> Unit,
    onClearDay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stateTips = tipsFor(state.hero.phase)
    val tipTitles: Array<String> =
        if (stateTips != null) stringArrayResource(stateTips.first) else emptyArray()
    val tipTexts: Array<String> =
        if (stateTips != null) stringArrayResource(stateTips.second) else emptyArray()
    val tipCount = minOf(tipTitles.size, tipTexts.size)

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
        if (state.isEmpty) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Filled.Favorite,
                    title = stringResource(R.string.cycle_empty_title),
                    message = stringResource(R.string.cycle_empty_message),
                    actionLabel = stringResource(R.string.cycle_empty_action),
                    onAction = onStartPeriodToday
                )
            }
        } else {
            item(key = "hero") { CycleHeroCard(hero = state.hero) }
        }

        item(key = "actions") {
            CycleActions(
                onPeriod = onOpenPeriodSheet,
                onSymptoms = onOpenSymptoms,
                onNote = { onOpenDay(state.today) }
            )
        }

        item(key = "calendar") {
            CycleCalendarCard(
                month = state.month,
                weekOffset = state.weekOffset,
                marks = state.marks,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onDayClick = onOpenDay
            )
        }

        if (!state.isEmpty) {
            item(key = "history") {
                CycleHistoryCard(history = state.history, currentDay = state.hero.cycleDay)
            }
            item(key = "chart") {
                CycleChartCard(chart = state.chart)
            }
            if (tipCount > 0) {
                item(key = "tips-title") {
                    SectionHeader(
                        title = stringResource(R.string.cycle_tips_title),
                        modifier = Modifier.padding(top = VdvoemSpacing.sm)
                    )
                }
                items(count = tipCount) { index ->
                    TipCard(title = tipTitles[index], message = tipTexts[index])
                }
            }
        }
    }

    when (val sheet = state.sheet) {
        is CycleSheet.Period -> PeriodSheet(
            onDismiss = onDismissSheet,
            onStartToday = onStartPeriodToday,
            onEndToday = onEndPeriodToday,
            onStartAt = onStartPeriodAt
        )

        is CycleSheet.Day -> DaySheet(
            draft = sheet.draft,
            isToday = sheet.draft.date == state.today,
            onDismiss = onDismissSheet,
            onUpdate = onUpdateDraft,
            onSave = onSaveDraft,
            onClear = onClearDay
        )

        null -> Unit
    }
}

@Composable
private fun CycleHeroCard(hero: CycleHero, modifier: Modifier = Modifier) {
    VdvoemCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            hero.phase?.let { phase ->
                InfoPill(text = phaseLabel(phase), icon = Icons.Filled.Favorite)
            }
            if (hero.isLate) {
                InfoPill(
                    text = stringResource(R.string.cycle_badge_late),
                    icon = Icons.Filled.Warning
                )
            }
        }
        Spacer(Modifier.height(VdvoemSpacing.md))
        Text(
            text = heroTitle(hero),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(Modifier.height(VdvoemSpacing.sm))
        Text(
            text = heroSubtitle(hero),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(Modifier.height(VdvoemSpacing.lg))
        LinearProgressIndicator(
            progress = { hero.progress },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        )
        hero.ovulationDate?.let { date ->
            Spacer(Modifier.height(VdvoemSpacing.md))
            Text(
                text = stringResource(R.string.cycle_ovulation_on, Dates.format(date)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun CycleActions(
    onPeriod: () -> Unit,
    onSymptoms: () -> Unit,
    onNote: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)) {
        OutlinedButton(onClick = onPeriod, modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = Icons.Filled.DateRange,
                contentDescription = null,
                modifier = Modifier.size(VdvoemSizes.iconMd)
            )
            Spacer(Modifier.width(VdvoemSpacing.sm))
            Text(stringResource(R.string.cycle_action_period))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)) {
            OutlinedButton(
                onClick = onSymptoms,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(VdvoemSizes.iconMd)
                )
                Spacer(Modifier.width(VdvoemSpacing.xs))
                Text(stringResource(R.string.cycle_action_symptoms), maxLines = 1)
            }
            OutlinedButton(
                onClick = onNote,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(VdvoemSizes.iconMd)
                )
                Spacer(Modifier.width(VdvoemSpacing.xs))
                Text(stringResource(R.string.cycle_action_note), maxLines = 1)
            }
        }
    }
}

@Composable
private fun CycleCalendarCard(
    month: YearMonth,
    weekOffset: Int,
    marks: Map<LocalDate, CycleDayMark>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit
) {
    VdvoemCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.cycle_calendar_title)
                )
            }
            Text(
                text = monthLabel(month),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onNextMonth) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.cycle_calendar_title)
                )
            }
        }
        CalendarWeekdayHeader()
        Spacer(Modifier.height(VdvoemSpacing.xs))
        val monthLength = month.lengthOfMonth()
        val rows = (weekOffset + monthLength + 6) / 7
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (column in 0 until 7) {
                    val dayOfMonth = row * 7 + column - weekOffset + 1
                    if (dayOfMonth in 1..monthLength) {
                        val date = month.atDay(dayOfMonth)
                        val mark = marks[date] ?: CycleDayMark(date = date)
                        Box(modifier = Modifier.weight(1f)) {
                            DayCell(mark = mark, onClick = { onDayClick(date) })
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        Spacer(Modifier.height(VdvoemSpacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.md)) {
            LegendItem(
                color = MaterialTheme.colorScheme.primary,
                label = stringResource(R.string.cycle_legend_period)
            )
            LegendItem(
                color = MaterialTheme.colorScheme.outline,
                label = stringResource(R.string.cycle_legend_forecast),
                outlined = true
            )
            LegendItem(
                color = MaterialTheme.colorScheme.tertiary,
                label = stringResource(R.string.cycle_legend_ovulation)
            )
        }
    }
}

@Composable
private fun CalendarWeekdayHeader() {
    val weekdays = stringArrayResource(R.array.cycle_weekdays)
    Row(modifier = Modifier.fillMaxWidth()) {
        weekdays.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DayCell(mark: CycleDayMark, onClick: () -> Unit) {
    val density = LocalDensity.current
    val strokePx = with(density) { VdvoemSizes.borderThin.toPx() }
    val dashPx = with(density) { VdvoemSpacing.xs.toPx() }
    val forecastColor = MaterialTheme.colorScheme.primary
    val todayColor = MaterialTheme.colorScheme.inverseSurface
    val container = when {
        mark.period -> MaterialTheme.colorScheme.primary
        mark.ovulation -> MaterialTheme.colorScheme.tertiary
        mark.fertile -> MaterialTheme.colorScheme.secondaryContainer
        else -> Color.Transparent
    }
    val content = when {
        mark.period -> MaterialTheme.colorScheme.onPrimary
        mark.ovulation -> MaterialTheme.colorScheme.onTertiary
        mark.fertile -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(VdvoemSpacing.huge)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (mark.forecast) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = forecastColor,
                    radius = (size.minDimension - strokePx) / 2f,
                    style = Stroke(
                        width = strokePx,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, dashPx))
                    )
                )
            }
        }
        Box(
            modifier = Modifier
                .size(VdvoemSpacing.xxxl)
                .background(container, CircleShape)
                .then(
                    if (mark.isToday) {
                        Modifier.border(VdvoemSizes.borderThin, todayColor, CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = mark.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = content,
                fontWeight = if (mark.isToday) FontWeight.Bold else FontWeight.Normal
            )
        }
        if (mark.hasRecord) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = VdvoemSpacing.xxs)
                    .size(VdvoemSpacing.xs)
                    .background(MaterialTheme.colorScheme.tertiary, CircleShape)
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, outlined: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.xs)
    ) {
        Box(
            modifier = Modifier
                .size(VdvoemSpacing.sm)
                .then(
                    if (outlined) {
                        Modifier.border(VdvoemSizes.borderThin, color, CircleShape)
                    } else {
                        Modifier.background(color, CircleShape)
                    }
                )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CycleHistoryCard(history: List<CycleHistoryItem>, currentDay: Int) {
    VdvoemCard {
        SectionHeader(title = stringResource(R.string.cycle_history_title))
        Spacer(Modifier.height(VdvoemSpacing.md))
        if (history.isEmpty()) {
            Text(
                text = stringResource(R.string.cycle_history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            history.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = VdvoemSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.cycle_history_length,
                            item.length,
                            item.length
                        ),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = stringResource(
                            R.string.cycle_history_range,
                            Dates.format(item.start),
                            Dates.format(item.end)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (currentDay > 0) {
            Spacer(Modifier.height(VdvoemSpacing.sm))
            HorizontalDivider(
                thickness = VdvoemSizes.borderThin,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(Modifier.height(VdvoemSpacing.sm))
            Text(
                text = stringResource(R.string.cycle_current_cycle, currentDay),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CycleChartCard(chart: CycleChart) {
    VdvoemCard {
        SectionHeader(title = stringResource(R.string.cycle_chart_title))
        Spacer(Modifier.height(VdvoemSpacing.md))
        if (chart.lengths.size < 2) {
            Text(
                text = stringResource(R.string.cycle_chart_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            CycleChartCanvas(lengths = chart.lengths, average = chart.average)
            Spacer(Modifier.height(VdvoemSpacing.lg))
            Row(modifier = Modifier.fillMaxWidth()) {
                ChartStat(
                    label = stringResource(R.string.cycle_chart_average),
                    value = chart.average,
                    modifier = Modifier.weight(1f)
                )
                ChartStat(
                    label = stringResource(R.string.cycle_chart_shortest),
                    value = chart.shortest,
                    modifier = Modifier.weight(1f)
                )
                ChartStat(
                    label = stringResource(R.string.cycle_chart_longest),
                    value = chart.longest,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ChartStat(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Простой график: столбики длины циклов и пунктир средней. Без сторонних библиотек. */
@Composable
private fun CycleChartCanvas(lengths: List<Int>, average: Int) {
    val barColor = MaterialTheme.colorScheme.primary
    val averageColor = MaterialTheme.colorScheme.tertiary
    val baselineColor = MaterialTheme.colorScheme.outlineVariant
    val density = LocalDensity.current
    val strokePx = with(density) { VdvoemSizes.borderThin.toPx() }
    val radiusPx = with(density) { VdvoemSpacing.xs.toPx() }
    val dashPx = with(density) { VdvoemSpacing.xs.toPx() }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(VdvoemSpacing.huge * 2)
    ) {
        if (lengths.isEmpty()) return@Canvas
        val lowest = (lengths.min() - 1).coerceAtLeast(1)
        val highest = lengths.max() + 1
        val range = (highest - lowest).coerceAtLeast(1).toFloat()
        val topPadding = size.height * 0.1f
        val usable = size.height - topPadding

        fun yFor(value: Int): Float =
            size.height - usable * ((value - lowest).toFloat() / range)

        val slot = size.width / lengths.size
        val barWidth = slot * 0.55f
        lengths.forEachIndexed { index, value ->
            val top = yFor(value)
            drawRoundRect(
                color = barColor,
                topLeft = Offset(index * slot + (slot - barWidth) / 2f, top),
                size = Size(barWidth, (size.height - top).coerceAtLeast(strokePx)),
                cornerRadius = CornerRadius(radiusPx, radiusPx)
            )
        }
        drawLine(
            color = baselineColor,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = strokePx
        )
        if (average > 0) {
            val averageY = yFor(average)
            drawLine(
                color = averageColor,
                start = Offset(0f, averageY),
                end = Offset(size.width, averageY),
                strokeWidth = strokePx,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx * 2f, dashPx))
            )
        }
    }
}

@Composable
private fun TipCard(title: String, message: String) {
    VdvoemCard(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(VdvoemSpacing.xs))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodSheet(
    onDismiss: () -> Unit,
    onStartToday: () -> Unit,
    onEndToday: () -> Unit,
    onStartAt: (Long) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VdvoemSpacing.screenHorizontal)
                .padding(bottom = VdvoemSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(VdvoemSpacing.sm)
        ) {
            Text(
                text = stringResource(R.string.cycle_sheet_period_title),
                style = MaterialTheme.typography.titleLarge
            )
            OutlinedButton(onClick = onStartToday, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(VdvoemSizes.iconMd)
                )
                Spacer(Modifier.width(VdvoemSpacing.sm))
                Text(stringResource(R.string.cycle_sheet_period_started))
            }
            OutlinedButton(onClick = onEndToday, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(VdvoemSizes.iconMd)
                )
                Spacer(Modifier.width(VdvoemSpacing.sm))
                Text(stringResource(R.string.cycle_sheet_period_ended))
            }
            OutlinedButton(
                onClick = { showPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(VdvoemSizes.iconMd)
                )
                Spacer(Modifier.width(VdvoemSpacing.sm))
                Text(stringResource(R.string.cycle_sheet_period_pick))
            }
            Text(
                text = stringResource(R.string.cycle_sheet_period_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            showPicker = false
                            onStartAt(millis)
                        }
                    }
                ) {
                    Text(stringResource(R.string.cycle_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.cycle_sheet_cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DaySheet(
    draft: CycleDayDraft,
    isToday: Boolean,
    onDismiss: () -> Unit,
    onUpdate: ((CycleDayDraft) -> CycleDayDraft) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Dates.format(draft.date),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                if (isToday) {
                    InfoPill(text = stringResource(R.string.cycle_sheet_day_today))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.cycle_sheet_period_toggle),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = draft.period,
                    onCheckedChange = { checked ->
                        onUpdate { current ->
                            current.copy(
                                period = checked,
                                flow = if (checked && current.flow == Flow.NONE) {
                                    Flow.MEDIUM
                                } else {
                                    current.flow
                                }
                            )
                        }
                    }
                )
            }
            if (draft.period) {
                Text(
                    text = stringResource(R.string.cycle_field_flow),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.xs)) {
                    listOf(Flow.NONE, Flow.LIGHT, Flow.MEDIUM, Flow.HEAVY).forEach { flow ->
                        FilterChip(
                            selected = draft.flow == flow,
                            onClick = { onUpdate { it.copy(flow = flow) } },
                            label = { Text(flowLabel(flow)) }
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.cycle_sheet_symptoms),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.xs)) {
                symptomKeys.forEach { key ->
                    val label = symptomLabel(key) ?: return@forEach
                    val selected = key in draft.symptoms
                    FilterChip(
                        selected = selected,
                        onClick = {
                            onUpdate { current ->
                                val next = current.symptoms.toMutableSet()
                                if (selected) next.remove(key) else next.add(key)
                                current.copy(symptoms = next)
                            }
                        },
                        label = { Text(label) }
                    )
                }
            }
            Text(
                text = stringResource(R.string.cycle_sheet_mood),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(VdvoemSpacing.xs)) {
                moodKeys.forEach { key ->
                    val label = moodLabel(key) ?: return@forEach
                    FilterChip(
                        selected = draft.mood == key,
                        onClick = {
                            onUpdate { current ->
                                current.copy(mood = if (current.mood == key) "" else key)
                            }
                        },
                        label = { Text(label) }
                    )
                }
            }
            OutlinedTextField(
                value = draft.weight,
                onValueChange = { value -> onUpdate { it.copy(weight = value) } },
                label = { Text(stringResource(R.string.cycle_sheet_weight)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.note,
                onValueChange = { value -> onUpdate { it.copy(note = value) } },
                label = { Text(stringResource(R.string.cycle_sheet_note)) },
                placeholder = { Text(stringResource(R.string.cycle_sheet_note_hint)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.cycle_sheet_save))
            }
            if (draft.hasRecord) {
                TextButton(onClick = onClear) {
                    Text(
                        text = stringResource(R.string.cycle_sheet_clear),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun flowLabel(flow: Flow): String = when (flow) {
    Flow.NONE -> stringResource(R.string.cycle_flow_none)
    Flow.LIGHT -> stringResource(R.string.cycle_flow_light)
    Flow.MEDIUM -> stringResource(R.string.cycle_flow_medium)
    Flow.HEAVY -> stringResource(R.string.cycle_flow_heavy)
}

@Composable
private fun symptomLabel(key: String): String? = when (key) {
    "cramps" -> stringResource(R.string.cycle_symptom_cramps)
    "headache" -> stringResource(R.string.cycle_symptom_headache)
    "backache" -> stringResource(R.string.cycle_symptom_backache)
    "acne" -> stringResource(R.string.cycle_symptom_acne)
    "bloating" -> stringResource(R.string.cycle_symptom_bloating)
    "fatigue" -> stringResource(R.string.cycle_symptom_fatigue)
    "nausea" -> stringResource(R.string.cycle_symptom_nausea)
    "breasts" -> stringResource(R.string.cycle_symptom_breasts)
    "sleep" -> stringResource(R.string.cycle_symptom_sleep)
    else -> null
}

@Composable
private fun moodLabel(key: String): String? = when (key) {
    "happy" -> stringResource(R.string.cycle_mood_happy)
    "calm" -> stringResource(R.string.cycle_mood_calm)
    "sad" -> stringResource(R.string.cycle_mood_sad)
    "irritable" -> stringResource(R.string.cycle_mood_irritable)
    "anxious" -> stringResource(R.string.cycle_mood_anxious)
    "energetic" -> stringResource(R.string.cycle_mood_energetic)
    else -> null
}

@Composable
private fun phaseLabel(phase: CyclePhase): String = when (phase) {
    CyclePhase.PERIOD -> stringResource(R.string.cycle_phase_period)
    CyclePhase.FOLLICULAR -> stringResource(R.string.cycle_phase_follicular)
    CyclePhase.OVULATION -> stringResource(R.string.cycle_phase_ovulation)
    CyclePhase.LUTEAL -> stringResource(R.string.cycle_phase_luteal)
}

@Composable
private fun heroTitle(hero: CycleHero): String {
    val until = hero.daysUntilPeriod
    return when {
        hero.periodToday -> stringResource(
            R.string.cycle_hero_period_day,
            hero.cycleDay.coerceAtLeast(1)
        )

        hero.isLate && until != null && until < 0 -> {
            val late = (-until).toInt()
            pluralStringResource(R.plurals.cycle_late, late, late)
        }

        until == null -> stringResource(R.string.cycle_hero_unknown)
        until <= 0L -> stringResource(R.string.cycle_hero_today)
        else -> pluralStringResource(R.plurals.cycle_days_left, until.toInt(), until.toInt())
    }
}

@Composable
private fun heroSubtitle(hero: CycleHero): String {
    val previous = hero.previousLength
    return when {
        previous != null -> pluralStringResource(
            R.plurals.cycle_cycle_lasted,
            previous,
            previous
        )

        hero.averageLength > 0 -> pluralStringResource(
            R.plurals.cycle_hero_average,
            hero.averageLength,
            hero.averageLength
        )

        else -> stringResource(R.string.cycle_hero_unknown)
    }
}

@Composable
private fun monthLabel(month: YearMonth): String {
    val locale = Locale.getDefault()
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("LLLL yyyy", locale) }
    val raw = month.atDay(1).format(formatter)
    return raw.replaceFirstChar { first ->
        if (first.isLowerCase()) first.titlecase(locale) else first.toString()
    }
}

@Preview(showBackground = true)
@Composable
private fun CycleHeroPreview() {
    VdvoemTheme {
        Column(modifier = Modifier.padding(VdvoemSpacing.screenHorizontal)) {
            CycleHeroCard(
                hero = CycleHero(
                    hasData = true,
                    periodToday = false,
                    cycleDay = 18,
                    daysUntilPeriod = 10,
                    phase = CyclePhase.LUTEAL,
                    previousLength = 29,
                    averageLength = 29,
                    periodLength = 5,
                    progress = 0.62f,
                    ovulationDate = LocalDate.now().minusDays(4)
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CycleCalendarPreview() {
    VdvoemTheme {
        val month = YearMonth.now()
        val marks = (1..month.lengthOfMonth()).associate { dayOfMonth ->
            val date = month.atDay(dayOfMonth)
            date to CycleDayMark(
                date = date,
                isToday = dayOfMonth == 16,
                period = dayOfMonth in 5..9,
                forecast = dayOfMonth in 26..30,
                ovulation = dayOfMonth == 13,
                fertile = dayOfMonth in 11..15,
                hasRecord = dayOfMonth == 18
            )
        }
        Column(modifier = Modifier.padding(VdvoemSpacing.screenHorizontal)) {
            CycleCalendarCard(
                month = month,
                weekOffset = month.atDay(1).dayOfWeek.value - 1,
                marks = marks,
                onPreviousMonth = {},
                onNextMonth = {},
                onDayClick = {}
            )
        }
    }
}