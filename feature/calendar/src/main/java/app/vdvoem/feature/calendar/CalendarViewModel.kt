package app.vdvoem.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vdvoem.core.domain.model.CalendarEvent
import app.vdvoem.core.domain.model.Pair
import app.vdvoem.core.domain.model.birthdays
import app.vdvoem.core.domain.model.upcomingDate
import app.vdvoem.core.domain.repository.CalendarRepository
import app.vdvoem.core.domain.repository.PairRepository
import app.vdvoem.core.domain.time.Dates
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

/** Фильтр списка по ближайшему вступлению события. */
enum class CalendarFilter { ALL, TODAY, WEEK, MONTH }

/** Порядок событий в списке. */
enum class CalendarSort { NEAREST, ALPHABET }

data class EventUi(
    val id: String,
    val pairId: String,
    val title: String,
    /** Ближайшее вступление: для годовщин — этот год или следующий. */
    val date: LocalDate,
    /** Исходная сохранённая дата — нужна для режима правки. */
    val dateMillis: Long,
    val annual: Boolean,
    val daysLeft: Long,
    /**
     * День рождения из профиля пары: показываем, но не редактируем —
     * дата меняется в профиле, а не в календаре.
     */
    val isBirthday: Boolean = false
)

data class CalendarUiState(
    val events: List<EventUi> = emptyList(),
    val totalCount: Int = 0,
    val filter: CalendarFilter = CalendarFilter.ALL,
    val sort: CalendarSort = CalendarSort.NEAREST,
    val next: EventUi? = null,
    /** Включены ли напоминания о событиях (DataStore модуля). */
    val remindersEnabled: Boolean = false,
    /** За сколько дней и в котором часу приходит напоминание. */
    val reminders: ReminderSettings = ReminderSettings(),
    val loading: Boolean = true
)

class CalendarViewModel(
    private val calendarRepository: CalendarRepository,
    private val pairRepository: PairRepository,
    private var reminderScheduler: ReminderScheduler? = null,
    private var reminderPreferences: ReminderPreferences? = null
) : ViewModel() {

    /** Состояние напоминаний: включается переключателем на экране календаря. */
    private val reminderSettings = MutableStateFlow(ReminderSettings())

    /** Последний полученный список — из него строится расписание напоминаний. */
    private var latestEvents: List<CalendarEvent> = emptyList()

    private var reminderJob: Job? = null

    init {
        if (reminderScheduler != null && reminderPreferences != null) observeReminders()
    }

    private val filter = MutableStateFlow(CalendarFilter.ALL)
    private val sort = MutableStateFlow(CalendarSort.NEAREST)

    val state: StateFlow<CalendarUiState> =
        combine(
            calendarRepository.observeEvents(),
            pairRepository.observe(),
            filter,
            sort,
            reminderSettings
        ) { events, pair, selected, order, reminders ->
            buildState(events, pair, selected, order, reminders)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = CalendarUiState()
            )

    /**
     * Подключает напоминания, если зависимости не пришли в конструкторе.
     * Экран календаря досоздаёт их из Koin, поэтому модуль работает без правок `app/`.
     */
    fun bindReminders(
        scheduler: ReminderScheduler,
        preferences: ReminderPreferences
    ) {
        if (reminderJob != null) return
        reminderScheduler = scheduler
        reminderPreferences = preferences
        observeReminders()
    }

    /** Включение/выключение напоминаний: сразу пересобираем или снимаем задачи. */
    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val preferences = reminderPreferences ?: return@launch
            preferences.setEnabled(enabled)
            val scheduler = reminderScheduler ?: return@launch
            val settings = reminderSettings.value.copy(enabled = enabled)
            if (enabled) scheduler.sync(latestEvents, settings) else scheduler.cancelAll()
        }
    }

    /** За сколько дней предупреждать — пересобираем расписание сразу. */
    fun setReminderLeadDays(days: Int) {
        viewModelScope.launch {
            val preferences = reminderPreferences ?: return@launch
            preferences.setLeadDays(days)
            val scheduler = reminderScheduler ?: return@launch
            val settings = reminderSettings.value.copy(leadDays = days)
            if (settings.enabled) scheduler.sync(latestEvents, settings)
        }
    }

    /** В котором часу присылать напоминания. */
    fun setReminderHour(hour: Int) {
        viewModelScope.launch {
            val preferences = reminderPreferences ?: return@launch
            preferences.setHour(hour)
            val scheduler = reminderScheduler ?: return@launch
            val settings = reminderSettings.value.copy(hour = hour)
            if (settings.enabled) scheduler.sync(latestEvents, settings)
        }
    }

    private fun observeReminders() {
        val preferences = reminderPreferences ?: return
        val scheduler = reminderScheduler ?: return
        // Настройка: включённые напоминания сразу получают расписание.
        reminderJob = viewModelScope.launch {
            preferences.settings.collect { settings ->
                reminderSettings.value = settings
                if (settings.enabled) scheduler.sync(latestEvents, settings) else scheduler.cancelAll()
            }
        }
        // Список событий: любое изменение (создание, правка, удаление) — повод пересобрать задачи.
        viewModelScope.launch {
            calendarRepository.observeEvents().collect { events ->
                latestEvents = events
                val settings = reminderSettings.value
                if (settings.enabled) scheduler.sync(events, settings)
            }
        }
    }

    fun setFilter(value: CalendarFilter) {
        filter.value = value
    }

    fun setSort(value: CalendarSort) {
        sort.value = value
    }

    /** Создаёт событие при `id == null`, иначе обновляет существующее. */
    fun saveEvent(id: String?, title: String, dateMillis: Long, annual: Boolean) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val pairId = pairRepository.current()?.id ?: LOCAL_PAIR_ID
            calendarRepository.upsert(
                CalendarEvent(
                    id = id ?: UUID.randomUUID().toString(),
                    pairId = pairId,
                    title = clean,
                    dateMillis = Dates.startOfDay(Dates.toLocalDate(dateMillis)),
                    annual = annual,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteEvent(id: String) {
        reminderScheduler?.cancel(id)
        viewModelScope.launch { calendarRepository.delete(id) }
    }

    private fun buildState(
        events: List<CalendarEvent>,
        pair: Pair?,
        selected: CalendarFilter,
        order: CalendarSort,
        reminders: ReminderSettings
    ): CalendarUiState {
        val today = Dates.today()
        val all = events.map { event ->
            val date = Dates.nextOccurrence(event.dateMillis, event.annual, today)
            EventUi(
                id = event.id,
                pairId = event.pairId,
                title = event.title,
                date = date,
                dateMillis = event.dateMillis,
                annual = event.annual,
                daysLeft = Dates.daysUntil(date, today)
            )
        } + pair.birthdayEvents(today)
        val byNearest = sortEvents(all)
        val visible = all.filter { it.matches(selected, today) }
        return CalendarUiState(
            events = when (order) {
                CalendarSort.NEAREST -> sortEvents(visible)
                CalendarSort.ALPHABET ->
                    visible.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            },
            totalCount = all.size,
            filter = selected,
            sort = order,
            remindersEnabled = reminders.enabled,
            reminders = reminders,
            // Ближайшее событие показываем в hero независимо от выбранного порядка.
            next = byNearest.firstOrNull { it.daysLeft >= 0 },
            loading = false
        )
    }

    /**
     * Ближайшее событие сверху, прошедшие — ниже и от свежих к старым.
     * Годовщины уже приведены к следующему вступлению, поэтому сортировка по дате корректна.
     */
    private fun sortEvents(items: List<EventUi>): List<EventUi> {
        val (upcoming, past) = items.partition { it.daysLeft >= 0 }
        val byDate = compareBy<EventUi>({ it.date }, { it.title })
        return upcoming.sortedWith(byDate) +
            past.sortedWith(compareByDescending<EventUi> { it.date }.thenBy { it.title })
    }

    private fun EventUi.matches(selected: CalendarFilter, today: LocalDate): Boolean =
        when (selected) {
            CalendarFilter.ALL -> true
            CalendarFilter.TODAY -> daysLeft == 0L
            CalendarFilter.WEEK -> daysLeft in 0..WEEK_FILTER_DAYS
            CalendarFilter.MONTH ->
                daysLeft >= 0 && date.year == today.year && date.month == today.month
        }

    /**
     * Дни рождения из профиля пары как события: без строки в базе они всегда
     * совпадают с профилем, а правка даты не требует ни миграции, ни синка.
     */
    private fun Pair?.birthdayEvents(today: LocalDate): List<EventUi> {
        val pair = this ?: return emptyList()
        return pair.birthdays().map { birthday ->
            val date = birthday.upcomingDate(today)
            EventUi(
                id = birthday.eventId,
                pairId = pair.id,
                title = birthday.name,
                date = date,
                dateMillis = birthday.dateMillis,
                annual = true,
                daysLeft = Dates.daysUntil(date, today),
                isBirthday = true
            )
        }
    }

    private companion object {
        const val LOCAL_PAIR_ID = "local"
        const val WEEK_FILTER_DAYS = 7L
    }
}