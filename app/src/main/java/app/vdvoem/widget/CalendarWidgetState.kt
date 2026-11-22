package app.vdvoem.widget

import android.content.Context
import app.vdvoem.R
import app.vdvoem.core.domain.model.CalendarEvent
import app.vdvoem.core.domain.model.Pair
import app.vdvoem.core.domain.model.birthdays
import app.vdvoem.core.domain.model.upcomingDate
import app.vdvoem.core.domain.repository.CalendarRepository
import app.vdvoem.core.domain.repository.PairRepository
import app.vdvoem.core.domain.time.Dates
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Одна строка виджета: название события и короткая дата. */
internal data class CalendarWidgetItem(
    val title: String,
    val whenLabel: String,
    /** Сегодня или завтра — подсвечиваем цветом. */
    val soon: Boolean
)

/** Готовый к отрисовке снимок виджета календаря. */
internal data class CalendarWidgetData(
    val headerLabel: String,
    val dateLabel: String,
    val emptyLabel: String,
    val items: List<CalendarWidgetItem>
)

/**
 * Читает события из репозитория и переводит их в подписи виджета.
 *
 * Как и у виджета цикла, Koin-контейнер уже поднят `VdvoemApplication`;
 * если данных нет или контейнер недоступен — показываем empty state.
 */
internal object CalendarWidgetState {

    suspend fun load(context: Context): CalendarWidgetData {
        val today = Dates.today()
        val events = runCatching {
            GlobalContext.get().get<CalendarRepository>().observeEvents().first()
        }.getOrDefault(emptyList())
        // Дни рождения в базе не лежат: они выводятся из профиля пары и идут в общий список.
        val pair = runCatching {
            GlobalContext.get().get<PairRepository>().observe().first()
        }.getOrNull()

        return CalendarWidgetData(
            headerLabel = context.getString(R.string.widget_calendar_label),
            dateLabel = today.format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault())),
            emptyLabel = context.getString(R.string.widget_calendar_empty),
            items = upcomingEvents(
                events,
                today,
                extra = birthdayItems(pair, today, context)
            )
                .map { event ->
                    CalendarWidgetItem(
                        title = event.title,
                        whenLabel = whenLabel(context, event.date, event.daysUntil),
                        soon = event.daysUntil <= 1L
                    )
                }
        )
    }

    /** Дни рождения из профиля в том же виде, что и обычные события виджета. */
    private fun birthdayItems(
        pair: Pair?,
        today: LocalDate,
        context: Context
    ): List<UpcomingEvent> =
        pair?.birthdays().orEmpty().map { birthday ->
            val date = birthday.upcomingDate(today)
            UpcomingEvent(
                title = context.getString(R.string.widget_calendar_birthday, birthday.name),
                date = date,
                daysUntil = Dates.daysUntil(date, today)
            )
        }

    private fun whenLabel(context: Context, date: LocalDate, daysUntil: Long): String = when (daysUntil) {
        0L -> context.getString(R.string.widget_calendar_today)
        1L -> context.getString(R.string.widget_calendar_tomorrow)
        else -> date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    }
}