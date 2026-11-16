package app.vdvoem.widget

import android.content.Context
import app.vdvoem.R
import app.vdvoem.core.domain.cycle.CyclePhase
import app.vdvoem.core.domain.cycle.CycleStats
import app.vdvoem.core.domain.cycle.buildForecast
import app.vdvoem.core.domain.model.CycleDay
import app.vdvoem.core.domain.model.CycleSettings
import app.vdvoem.core.domain.repository.CycleRepository
import app.vdvoem.core.domain.time.Dates
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Сколько дней вперёд показываем точками под прогнозом. */
private const val MARKER_DAYS = 5

/** Разметка дня в мини-шкале виджета. */
internal enum class CycleDayMarker { PERIOD, FORECAST, NONE }

/**
 * Готовый к отрисовке снимок виджета.
 *
 * Все подписи собираются здесь, до композиции: так Glance-контент остаётся простым,
 * а текст всегда на языке системы.
 */
internal data class CycleWidgetData(
    val dateLabel: String,
    /** `null`, когда месячные ещё не отмечены: тогда виджет показывает только подсказку. */
    val dayLabel: String?,
    val phaseLabel: String?,
    val hintLabel: String,
    val markers: List<CycleDayMarker>
)

/**
 * Читает данные цикла из репозитория и переводит прогноз в подписи виджета.
 *
 * Виджет живёт в процессе приложения, поэтому берёт зависимости из общего Koin-контейнера.
 * Если контейнер ещё не поднялся (например, процесс разбудил только AppWidget),
 * виджет не падает, а показывает empty state.
 */
internal object CycleWidgetState {

    suspend fun load(context: Context): CycleWidgetData {
        val snapshot = runCatching {
            val repository = GlobalContext.get().get<CycleRepository>()
            combine(repository.observeDays(), repository.observeSettings()) { days, settings ->
                CycleSnapshot(days, settings)
            }.first()
        }.getOrNull()

        val today = Dates.today()
        val days = snapshot?.days.orEmpty()
        val settings = snapshot?.settings
        val stats = buildForecast(days = days, settings = settings, today = today)

        // Удалённые дни не считаем: buildForecast их тоже игнорирует.
        val periodDates = days.asSequence()
            .filter { it.deletedAt == null && it.period }
            .map { Dates.toLocalDate(it.dateMillis) }
            .toSet()

        val known = stats.lastPeriodStart != null

        return CycleWidgetData(
            dateLabel = today.format(
                DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault())
            ),
            dayLabel = if (known) context.getString(R.string.widget_cycle_day, stats.currentDay) else null,
            phaseLabel = if (known) context.getString(phaseName(stats.phase)) else null,
            hintLabel = hint(context, stats),
            markers = markers(today = today, periodDates = periodDates, stats = stats)
        )
    }

    private fun phaseName(phase: CyclePhase): Int = when (phase) {
        CyclePhase.PERIOD -> R.string.widget_cycle_phase_period
        CyclePhase.FOLLICULAR -> R.string.widget_cycle_phase_follicular
        CyclePhase.OVULATION -> R.string.widget_cycle_phase_ovulation
        CyclePhase.LUTEAL -> R.string.widget_cycle_phase_luteal
    }

    /** Одна короткая строка под днём цикла: что происходит сейчас. */
    private fun hint(context: Context, stats: CycleStats): String {
        val daysUntil = stats.daysUntilNextPeriod
        return when {
            stats.lastPeriodStart == null ->
                context.getString(R.string.widget_cycle_empty)

            stats.phase == CyclePhase.PERIOD ->
                context.getString(R.string.widget_cycle_period_now)

            stats.isLate ->
                context.getString(
                    R.string.widget_cycle_late,
                    (stats.currentDay - stats.averageLength).coerceAtLeast(1)
                )

            daysUntil == null ->
                context.getString(R.string.widget_cycle_empty)

            daysUntil <= 0L ->
                context.getString(R.string.widget_cycle_period_today)

            else ->
                context.getString(R.string.widget_cycle_until_period, daysUntil.toInt())
        }
    }

    /**
     * Пять дней начиная с сегодня: отмеченные месячные, прогноз месячных или обычный день.
     * Хвост длинного месяца не выходим за пределы — просто пять точек.
     */
    private fun markers(
        today: LocalDate,
        periodDates: Set<LocalDate>,
        stats: CycleStats
    ): List<CycleDayMarker> {
        val forecastStart = stats.nextPeriodStart
        val forecastEnd = forecastStart?.plusDays(stats.periodLength.toLong())

        return (0 until MARKER_DAYS).map { offset ->
            val date = today.plusDays(offset.toLong())
            when {
                date in periodDates -> CycleDayMarker.PERIOD
                forecastStart != null && forecastEnd != null &&
                    !date.isBefore(forecastStart) && date.isBefore(forecastEnd) ->
                    CycleDayMarker.FORECAST

                else -> CycleDayMarker.NONE
            }
        }
    }

    private data class CycleSnapshot(
        val days: List<CycleDay>,
        val settings: CycleSettings?
    )
}