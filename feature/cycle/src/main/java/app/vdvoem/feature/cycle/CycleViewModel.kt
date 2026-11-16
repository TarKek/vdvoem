package app.vdvoem.feature.cycle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vdvoem.core.domain.cycle.CyclePhase
import app.vdvoem.core.domain.cycle.CycleStats
import app.vdvoem.core.domain.cycle.buildForecast
import app.vdvoem.core.domain.model.CycleDay
import app.vdvoem.core.domain.model.CycleSettings
import app.vdvoem.core.domain.model.Flow
import app.vdvoem.core.domain.repository.CycleRepository
import app.vdvoem.core.domain.time.Dates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID

/** Один день в сетке календаря: что показывать в кружке. */
data class CycleDayMark(
    val date: LocalDate,
    val isToday: Boolean = false,
    val period: Boolean = false,
    val forecast: Boolean = false,
    val ovulation: Boolean = false,
    val fertile: Boolean = false,
    val hasRecord: Boolean = false,
    val symptoms: List<String> = emptyList()
)

/** Завершённый цикл в истории. */
data class CycleHistoryItem(
    val length: Int,
    val start: LocalDate,
    val end: LocalDate
)

/** Данные hero-карточки, уже готовые к показу. */
data class CycleHero(
    val hasData: Boolean = false,
    val periodToday: Boolean = false,
    val cycleDay: Int = 0,
    val daysUntilPeriod: Long? = null,
    val isLate: Boolean = false,
    val phase: CyclePhase? = null,
    val previousLength: Int? = null,
    val averageLength: Int = 0,
    val periodLength: Int = 0,
    val progress: Float = 0f,
    val ovulationDate: LocalDate? = null
)

/** Данные графика длины цикла. */
data class CycleChart(
    val lengths: List<Int> = emptyList(),
    val average: Int = 0,
    val shortest: Int = 0,
    val longest: Int = 0
)

/** Черновик записи дня в шторке. */
data class CycleDayDraft(
    val date: LocalDate,
    val period: Boolean = false,
    val flow: Flow = Flow.NONE,
    val symptoms: Set<String> = emptySet(),
    val mood: String = "",
    val weight: String = "",
    val note: String = "",
    val hasRecord: Boolean = false
)

/** Открытая шторка: выбор месячных или запись конкретного дня. */
sealed interface CycleSheet {
    data object Period : CycleSheet

    data class Day(val draft: CycleDayDraft) : CycleSheet
}

data class CycleUiState(
    val today: LocalDate = LocalDate.now(),
    val hero: CycleHero = CycleHero(),
    val month: YearMonth = YearMonth.now(),
    val weekOffset: Int = YearMonth.now().atDay(1).dayOfWeek.value - 1,
    val marks: Map<LocalDate, CycleDayMark> = emptyMap(),
    val history: List<CycleHistoryItem> = emptyList(),
    val chart: CycleChart = CycleChart(),
    val sheet: CycleSheet? = null,
    val isEmpty: Boolean = true
) {
    /** Число дней в показанном месяце. */
    val monthLength: Int
        get() = month.lengthOfMonth()
}

/**
 * Состояние экрана «Цикл».
 *
 * Прогноз считает домен ([buildForecast]), а ViewModel только раскладывает его по карточкам:
 * hero, календарь, история, график. Все записи идут через [CycleRepository] — данные общие
 * с партнёром.
 */
class CycleViewModel(
    private val repository: CycleRepository
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    private val sheet = MutableStateFlow<CycleSheet?>(null)
    private val _state = MutableStateFlow(CycleUiState())

    val state: StateFlow<CycleUiState> = _state.asStateFlow()

    private var lastDays: List<CycleDay> = emptyList()
    private var lastSettings: CycleSettings? = null

    init {
        viewModelScope.launch {
            combine(
                repository.observeDays(),
                repository.observeSettings(),
                month,
                sheet
            ) { days, settings, visibleMonth, openedSheet ->
                val active = days.filter { it.deletedAt == null }
                lastDays = active
                lastSettings = settings
                buildState(active, settings, visibleMonth, openedSheet)
            }.collect { _state.value = it }
        }
    }

    fun previousMonth() {
        month.value = month.value.minusMonths(1)
    }

    fun nextMonth() {
        month.value = month.value.plusMonths(1)
    }

    fun openPeriodSheet() {
        sheet.value = CycleSheet.Period
    }

    /** Шторка симптомов всегда открывается на сегодняшнем дне. */
    fun openSymptoms() {
        openDay(Dates.today())
    }

    fun openDay(date: LocalDate) {
        val record = lastDays.firstOrNull { Dates.toLocalDate(it.dateMillis) == date }
        sheet.value = CycleSheet.Day(
            CycleDayDraft(
                date = date,
                period = record?.period == true,
                flow = record?.flow ?: Flow.NONE,
                symptoms = record?.symptoms?.toSet().orEmpty(),
                mood = record?.mood.orEmpty(),
                weight = record?.weight?.let(::formatWeight).orEmpty(),
                note = record?.note.orEmpty(),
                hasRecord = record != null
            )
        )
    }

    fun dismissSheet() {
        sheet.value = null
    }

    fun updateDraft(transform: (CycleDayDraft) -> CycleDayDraft) {
        val current = sheet.value as? CycleSheet.Day ?: return
        sheet.value = CycleSheet.Day(transform(current.draft))
    }

    fun saveDraft() {
        val draft = (sheet.value as? CycleSheet.Day)?.draft ?: return
        val existing = lastDays.firstOrNull { Dates.toLocalDate(it.dateMillis) == draft.date }
        val flow = if (draft.period) draft.flow else Flow.NONE
        val record = CycleDay(
            id = existing?.id ?: UUID.randomUUID().toString(),
            pairId = pairIdFor(existing),
            dateMillis = Dates.startOfDay(draft.date),
            period = draft.period,
            flow = flow,
            symptoms = draft.symptoms.toList().sorted(),
            mood = draft.mood,
            weight = draft.weight.trim().replace(',', '.').toFloatOrNull(),
            note = draft.note.trim(),
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            when {
                !record.isEmptyRecord() -> repository.saveDay(record)
                existing != null -> repository.saveDay(existing.tombstone())
            }
            sheet.value = null
        }
    }

    fun clearDay() {
        val draft = (sheet.value as? CycleSheet.Day)?.draft ?: return
        val existing = lastDays.firstOrNull { Dates.toLocalDate(it.dateMillis) == draft.date }
        viewModelScope.launch {
            if (existing != null) repository.saveDay(existing.tombstone())
            sheet.value = null
        }
    }

    fun startPeriodToday() {
        startPeriod(Dates.today())
    }

    /** Дата из DatePicker приходит полуночью UTC. */
    fun startPeriodAt(pickerMillis: Long) {
        startPeriod(Dates.toLocalDate(Dates.fromPickerMillis(pickerMillis)))
    }

    fun endPeriodToday() {
        viewModelScope.launch {
            val today = Dates.today()
            val periods = lastDays.filter { it.period }.sortedBy { it.dateMillis }
            val lastPeriod = periods.lastOrNull()
            val lastDate = lastPeriod?.let { Dates.toLocalDate(it.dateMillis) }
            if (lastPeriod == null || lastDate == null || lastDate.isBefore(today)) {
                sheet.value = null
                return@launch
            }
            val periodDates = periods.mapTo(HashSet()) { Dates.toLocalDate(it.dateMillis) }
            var length = 1
            var cursor = lastDate.minusDays(1)
            while (periodDates.contains(cursor)) {
                length += 1
                cursor = cursor.minusDays(1)
            }
            val cleared = lastPeriod.copy(
                period = false,
                flow = Flow.NONE,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveDay(if (cleared.isEmptyRecord()) cleared.tombstone() else cleared)
            lastSettings?.let { settings ->
                val updated = settings.copy(
                    periodLength = length.coerceIn(1, 14),
                    updatedAt = System.currentTimeMillis()
                )
                repository.saveSettings(updated)
            }
            sheet.value = null
        }
    }

    private fun startPeriod(date: LocalDate) {
        viewModelScope.launch {
            val existing = lastDays.firstOrNull { Dates.toLocalDate(it.dateMillis) == date }
            val base = existing ?: CycleDay(
                id = UUID.randomUUID().toString(),
                pairId = pairIdFor(null),
                dateMillis = Dates.startOfDay(date)
            )
            val flow = existing?.flow?.takeIf { it != Flow.NONE } ?: Flow.MEDIUM
            repository.saveDay(
                base.copy(
                    period = true,
                    flow = flow,
                    updatedAt = System.currentTimeMillis(),
                    deletedAt = null
                )
            )
            sheet.value = null
        }
    }

    private fun pairIdFor(existing: CycleDay?): String =
        existing?.pairId?.takeIf { it.isNotBlank() }
            ?: lastSettings?.pairId?.takeIf { it.isNotBlank() }
            ?: lastDays.firstOrNull()?.pairId.orEmpty()

    private fun buildState(
        days: List<CycleDay>,
        settings: CycleSettings?,
        visibleMonth: YearMonth,
        openedSheet: CycleSheet?
    ): CycleUiState {
        val today = Dates.today()
        val hasPeriod = days.any { it.period }
        val stats = if (hasPeriod) buildForecast(days, settings, today) else null
        val periodToday = days.any { it.period && Dates.toLocalDate(it.dateMillis) == today }
        val average = stats?.averageLength ?: settings?.cycleLength ?: 0
        val cycleDay = stats?.currentDay ?: 0
        val hero = CycleHero(
            hasData = hasPeriod,
            periodToday = periodToday,
            cycleDay = cycleDay,
            daysUntilPeriod = stats?.daysUntilNextPeriod,
            isLate = stats?.isLate == true,
            phase = stats?.phase,
            previousLength = stats?.lengths?.lastOrNull(),
            averageLength = average,
            periodLength = stats?.periodLength ?: settings?.periodLength ?: 0,
            progress = if (average > 0) (cycleDay.toFloat() / average).coerceIn(0f, 1f) else 0f,
            ovulationDate = stats?.ovulationDate
        )
        return CycleUiState(
            today = today,
            hero = hero,
            month = visibleMonth,
            weekOffset = visibleMonth.atDay(1).dayOfWeek.value - 1,
            marks = buildMarks(days, stats, visibleMonth, today),
            history = buildHistory(days),
            chart = CycleChart(
                lengths = stats?.lengths.orEmpty(),
                average = average,
                shortest = stats?.shortestLength ?: 0,
                longest = stats?.longestLength ?: 0
            ),
            sheet = openedSheet,
            isEmpty = !hasPeriod
        )
    }

    private fun buildMarks(
        days: List<CycleDay>,
        stats: CycleStats?,
        visibleMonth: YearMonth,
        today: LocalDate
    ): Map<LocalDate, CycleDayMark> {
        val byDate = days.associateBy { Dates.toLocalDate(it.dateMillis) }
        val forecast = HashSet<LocalDate>()
        val nextStart = stats?.nextPeriodStart
        val periodLength = (stats?.periodLength ?: lastSettings?.periodLength ?: 5).coerceIn(1, 14)
        if (nextStart != null) {
            for (offset in 0 until periodLength) {
                forecast += nextStart.plusDays(offset.toLong())
            }
        }
        val fertile = HashSet<LocalDate>()
        val fertileStart: LocalDate? = stats?.fertileStart
        val fertileEnd: LocalDate? = stats?.fertileEnd
        if (fertileStart != null && fertileEnd != null) {
            var cursor: LocalDate = fertileStart
            while (!cursor.isAfter(fertileEnd)) {
                fertile += cursor
                cursor = cursor.plusDays(1)
            }
        }
        val ovulation = stats?.ovulationDate
        val result = LinkedHashMap<LocalDate, CycleDayMark>()
        for (dayOfMonth in 1..visibleMonth.lengthOfMonth()) {
            val date = visibleMonth.atDay(dayOfMonth)
            val record = byDate[date]
            result[date] = CycleDayMark(
                date = date,
                isToday = date == today,
                period = record?.period == true,
                forecast = forecast.contains(date) && record?.period != true,
                ovulation = ovulation != null && ovulation == date,
                fertile = fertile.contains(date),
                hasRecord = record?.hasAnyContent() == true,
                symptoms = record?.symptoms.orEmpty()
            )
        }
        return result
    }

    private fun buildHistory(days: List<CycleDay>): List<CycleHistoryItem> {
        val starts = cycleStarts(days)
        if (starts.size < 2) return emptyList()
        return starts.zipWithNext { from, to ->
            CycleHistoryItem(
                length = ChronoUnit.DAYS.between(from, to).toInt(),
                start = from,
                end = to.minusDays(1)
            )
        }
            .filter { it.length in 10..90 }
            .sortedByDescending { it.start }
            .take(6)
    }

    /** Начала циклов: день месячных, у которого предыдущий день не был днём месячных. */
    private fun cycleStarts(days: List<CycleDay>): List<LocalDate> {
        val periodDates = days.filter { it.period }
            .map { Dates.toLocalDate(it.dateMillis) }
            .distinct()
            .sorted()
        val result = mutableListOf<LocalDate>()
        periodDates.forEachIndexed { index, date ->
            if (index == 0 || ChronoUnit.DAYS.between(periodDates[index - 1], date) > 1) {
                result += date
            }
        }
        return result
    }

    private fun CycleDay.hasAnyContent(): Boolean =
        period || flow != Flow.NONE || symptoms.isNotEmpty() ||
            mood.isNotBlank() || weight != null || note.isNotBlank()

    private fun CycleDay.isEmptyRecord(): Boolean = !hasAnyContent()

    private fun CycleDay.tombstone(): CycleDay = copy(deletedAt = System.currentTimeMillis())

    private fun formatWeight(value: Float): String =
        String.format(Locale.getDefault(), "%.1f", value)
}

/** Фаза → подписи для карточек советов. */
internal fun tipsFor(phase: CyclePhase?): Pair<Int, Int>? = when (phase) {
    CyclePhase.PERIOD -> R.array.cycle_tips_period to R.array.cycle_tips_period_text
    CyclePhase.FOLLICULAR -> R.array.cycle_tips_follicular to R.array.cycle_tips_follicular_text
    CyclePhase.OVULATION -> R.array.cycle_tips_ovulation to R.array.cycle_tips_ovulation_text
    CyclePhase.LUTEAL -> R.array.cycle_tips_luteal to R.array.cycle_tips_luteal_text
    null -> null
}