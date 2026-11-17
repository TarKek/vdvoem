package app.vdvoem.core.domain.cycle

import app.vdvoem.core.domain.model.CycleDay
import app.vdvoem.core.domain.model.CycleSettings
import app.vdvoem.core.domain.time.Dates
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** Фаза цикла на сегодня. */
enum class CyclePhase { PERIOD, FOLLICULAR, OVULATION, LUTEAL }

/**
 * Прогноз по дню цикла.
 *
 * Поля nullable там, где данных не хватает: экран по ним рисует empty state
 * («Отметьте начало месячных»), а не выдумывает даты.
 */
data class CycleStats(
    /** 1..N, день текущего цикла. Без данных — 1. */
    val currentDay: Int,
    val phase: CyclePhase,
    val lastPeriodStart: LocalDate?,
    val nextPeriodStart: LocalDate?,
    val daysUntilNextPeriod: Long?,
    val ovulationDate: LocalDate?,
    /** Окно ±2 дня от овуляции. */
    val fertileStart: LocalDate?,
    val fertileEnd: LocalDate?,
    /** История длин завершённых циклов, старые → новые. */
    val lengths: List<Int>,
    val averageLength: Int,
    val shortestLength: Int,
    val longestLength: Int,
    val periodLength: Int,
    /** Текущий цикл длиннее средней более чем на 3 дня. */
    val isLate: Boolean
)

/** Длина цикла и месячных по умолчанию, если настроек ещё нет. */
const val DEFAULT_CYCLE_LENGTH = 28
const val DEFAULT_PERIOD_LENGTH = 5

/** Насколько цикл должен превысить среднюю длину, чтобы считаться задержавшимся. */
private const val LATE_TOLERANCE_DAYS = 3

/** Овуляция наступает за столько дней до следующего начала месячных. */
private const val LUTEAL_PHASE_DAYS = 14

/** Ширина фертильного окна в каждую сторону от овуляции. */
private const val FERTILE_WINDOW_DAYS = 2

/**
 * Прогноз цикла — чистый Kotlin, без Android и без обращения к «сегодня» из системы
 * (дата приходит параметром, поэтому правила покрываются JVM-тестами).
 *
 * Правила:
 * - начало цикла — день `period = true`, у которого предыдущий день не был днём месячных
 *   (или между ними разрыв больше суток);
 * - длина цикла — расстояние между соседними началами; текущий (незавершённый) цикл
 *   в историю не попадает;
 * - средняя длина — по завершённым циклам, но только если их хотя бы два, иначе
 *   ожидаемая длина из настроек;
 * - овуляция — за 14 дней до следующего начала;
 * - задержка — текущий день больше средней длины более чем на 3 дня;
 * - удалённые ([CycleDay.deletedAt] != null) и пустые дни не учитываются.
 */
fun buildForecast(
    days: List<CycleDay>,
    settings: CycleSettings?,
    today: LocalDate = Dates.today()
): CycleStats {
    val periodDates = days.asSequence()
        .filter { it.deletedAt == null && it.period }
        .map { Dates.toLocalDate(it.dateMillis) }
        .distinct()
        .sorted()
        .toList()

    // Начало цикла: день месячных, перед которым месячных не было.
    val starts = periodDates.filterIndexed { index, date ->
        index == 0 || date != periodDates[index - 1].plusDays(1)
    }

    val lengths = starts.zipWithNext { from, to ->
        ChronoUnit.DAYS.between(from, to).toInt()
    }.filter { it > 0 }

    val expectedCycle = settings?.cycleLength?.takeIf { it > 0 } ?: DEFAULT_CYCLE_LENGTH
    val periodLength = settings?.periodLength?.takeIf { it > 0 } ?: DEFAULT_PERIOD_LENGTH

    val averageLength = if (lengths.size >= 2) lengths.average().roundToInt() else expectedCycle
    val shortestLength = lengths.minOrNull() ?: averageLength
    val longestLength = lengths.maxOrNull() ?: averageLength

    // Текущий цикл — последнее начало не позже сегодняшнего дня. Если все начала
    // в будущем (пользователь отметил дату заранее), берём ближайшее.
    val lastStart = starts.lastOrNull { !it.isAfter(today) }

    val currentDay = if (lastStart == null) {
        1
    } else {
        val elapsed = ChronoUnit.DAYS.between(lastStart, today).toInt() + 1
        elapsed.coerceAtLeast(1)
    }

    val nextPeriodStart = lastStart?.plusDays(averageLength.toLong())
        ?: starts.firstOrNull { it.isAfter(today) }
    val ovulationDate = nextPeriodStart?.minusDays(LUTEAL_PHASE_DAYS.toLong())
    val fertileStart = ovulationDate?.minusDays(FERTILE_WINDOW_DAYS.toLong())
    val fertileEnd = ovulationDate?.plusDays(FERTILE_WINDOW_DAYS.toLong())
    val daysUntilNextPeriod = nextPeriodStart?.let { ChronoUnit.DAYS.between(today, it) }

    val phase = when {
        today in periodDates -> CyclePhase.PERIOD
        fertileStart == null || fertileEnd == null -> CyclePhase.FOLLICULAR
        today.isBefore(fertileStart) -> CyclePhase.FOLLICULAR
        today.isAfter(fertileEnd) -> CyclePhase.LUTEAL
        else -> CyclePhase.OVULATION
    }

    return CycleStats(
        currentDay = currentDay,
        phase = phase,
        lastPeriodStart = lastStart,
        nextPeriodStart = nextPeriodStart,
        daysUntilNextPeriod = daysUntilNextPeriod,
        ovulationDate = ovulationDate,
        fertileStart = fertileStart,
        fertileEnd = fertileEnd,
        lengths = lengths,
        averageLength = averageLength,
        shortestLength = shortestLength,
        longestLength = longestLength,
        periodLength = periodLength,
        isLate = lastStart != null && currentDay > averageLength + LATE_TOLERANCE_DAYS
    )
}