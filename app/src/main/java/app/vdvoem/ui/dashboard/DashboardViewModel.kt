package app.vdvoem.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vdvoem.core.domain.model.CalendarEvent
import app.vdvoem.core.domain.model.Lesson
import app.vdvoem.core.domain.model.Pair
import app.vdvoem.core.domain.model.Poke
import app.vdvoem.core.domain.model.birthdays
import app.vdvoem.core.domain.model.upcomingDate
import app.vdvoem.core.domain.cycle.CycleStats
import app.vdvoem.core.domain.cycle.buildForecast
import app.vdvoem.core.domain.repository.CalendarRepository
import app.vdvoem.core.domain.repository.CycleRepository
import app.vdvoem.core.domain.repository.GiftRepository
import app.vdvoem.core.domain.repository.LessonRepository
import app.vdvoem.core.domain.repository.PairRepository
import app.vdvoem.core.domain.repository.PokeRepository
import app.vdvoem.core.domain.device.DeviceIdentity
import app.vdvoem.core.domain.repository.WishRepository
import app.vdvoem.core.domain.time.Dates
import app.vdvoem.core.domain.settings.ReminderLeadSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

/** Ближайшее событие календаря или день рождения пары и сколько дней до него: 0 — сегодня. */
data class TodayEvent(
    val title: String,
    val daysUntil: Long,
    val isBirthday: Boolean = false
)

data class DashboardUiState(
    val pair: Pair? = null,
    val events: List<CalendarEvent> = emptyList(),
    val nextEvent: CalendarEvent? = null,
    val eventCount: Int = 0,
    val wishCount: Int = 0,
    val giftCount: Int = 0,
    val lastPoke: Poke? = null,
    val pokes: List<Poke> = emptyList(),
    /** Прогноз цикла для карточки-виджета; null, если данных ещё нет. */
    val cycleStats: CycleStats? = null,
    /** Текущая дата: обновляется в полночь, чтобы «Сегодня» не замирало на вчера. */
    val today: LocalDate = Dates.today(),
    /** Уроки сегодняшнего дня недели — для кнопки «Сегодня» на карточке пары. */
    val todayLessons: List<Lesson> = emptyList(),
    /** Ближайшие события в пределах срока напоминаний (0 — сегодня). */
    val upcomingEvents: List<TodayEvent> = emptyList()
)

class DashboardViewModel(
    private val pairRepository: PairRepository,
    private val calendarRepository: CalendarRepository,
    private val wishRepository: WishRepository,
    private val giftRepository: GiftRepository,
    private val pokeRepository: PokeRepository,
    private val cycleRepository: CycleRepository,
    private val lessonRepository: LessonRepository,
    private val reminderLeadSettings: ReminderLeadSettings,
    private val deviceIdentity: DeviceIdentity
) : ViewModel() {

    /**
     * Текущая дата отдельным потоком: приложение может висеть открытым через полночь,
     * и тогда «сегодня» должно переехать само — и в уроках, и в заголовке шторки.
     */
    private val todayFlow = flow {
        while (true) {
            val today = Dates.today()
            emit(today)
            val nextMidnight = today.plusDays(1).atStartOfDay(ZoneId.systemDefault())
            val waitMs = Duration.between(LocalDateTime.now(), nextMidnight).toMillis()
            delay(waitMs.coerceAtLeast(1_000L) + 1_000L)
        }
    }.distinctUntilChanged()

    /** Уроки сегодняшнего дня недели: расписание циклическое, привязка — к дню недели. */
    private val todayLessonsFlow = combine(lessonRepository.observe(), todayFlow) { lessons, today ->
        val weekday = today.dayOfWeek.value
        lessons
            .filter { it.deletedAt == null && it.weekday == weekday }
            .sortedBy { it.startMinute }
    }

    /** Дата и уроки вместе: в состоянии они всегда согласованы. */
    private val todayStateFlow = combine(todayFlow, todayLessonsFlow) { today, lessons ->
        today to lessons
    }

    /** Срок напоминаний: за сколько дней до события показывать его на «Главной». */
    private val reminderLeadDaysFlow = reminderLeadSettings.leadDays
        .catch { emit(ReminderLeadSettings.DEFAULT_LEAD_DAYS) }
        .distinctUntilChanged()

    /** Прогноз цикла отдельным потоком: он не должен пересчитывать всю «Главную». */
    private val cycleFlow = combine(
        cycleRepository.observeDays(),
        cycleRepository.observeSettings()
    ) { days, settings ->
        // Без настроек считаем по умолчанию (28/5) — так же, как экран «Цикл».
        buildForecast(days, settings, Dates.today())
    }

    val state = combine(
        pairRepository.observe(),
        calendarRepository.observeEvents(),
        wishRepository.observeWishes(),
        giftRepository.observeGifts(),
        pokeRepository.observe()
    ) { pair, events, wishes, gifts, pokes ->
        // Желание-сюрприз партнёра не должно попадать даже в счётчик на главной.
        val myDevice = deviceIdentity.deviceId()
        val visibleWishes = wishes.filterNot { wish ->
            wish.surprise && wish.ownerDeviceId.isNotEmpty() && wish.ownerDeviceId != myDevice
        }
        val visibleGifts = gifts.filter { gift ->
            !gift.surprise || gift.status == app.vdvoem.core.domain.model.GiftStatus.GIVEN ||
                gift.ownerDeviceId == myDevice
        }
        val today = Dates.today()
        val dated = events.map { event ->
            event to Dates.nextOccurrence(event.dateMillis, event.annual, today)
        }
        // Сначала ближайшее сегодняшнее/будущее; если таких нет — самое свежее прошедшее,
        // чтобы виджет не показывал отрицательные дни вроде «-1 день».
        val upcoming = dated.filter { (_, date) -> !date.isBefore(today) }
            .minByOrNull { (_, date) -> date }
        val latestPast = dated.maxByOrNull { (_, date) -> date }
        DashboardUiState(
            pair = pair,
            events = events.sortedBy { it.dateMillis },
            nextEvent = (upcoming ?: latestPast)?.first,
            // Дни рождения из профиля пары — тоже события: иначе их приходилось
            // заводить в календаре руками, а теперь они появляются там сами.
            eventCount = events.size +
                pair?.birthdays().orEmpty().size,
            wishCount = visibleWishes.size,
            giftCount = visibleGifts.size,
            lastPoke = pokes.firstOrNull(),
            pokes = pokes
        )
    }.combine(cycleFlow) { dashboard, cycle ->
        dashboard.copy(cycleStats = cycle)
    }.combine(todayStateFlow) { dashboard, (today, lessons) ->
        dashboard.copy(today = today, todayLessons = lessons)
    }.combine(reminderLeadDaysFlow) { dashboard, leadDays ->
        // Срок берём из настроек напоминаний календаря: за столько дней до события
        // показываем его на «Главной» и зажигаем точку на кнопке «Сегодня».
        val today = dashboard.today
        val limit = leadDays.toLong()
        val events = dashboard.events.mapNotNull { event ->
            val days = Dates.daysUntil(Dates.nextOccurrence(event.dateMillis, event.annual, today), today)
            if (days in 0..limit) TodayEvent(event.title, days) else null
        }
        // Дни рождения живут в профиле пары, но в календаре показываются как события,
        // поэтому и в «Сегодня» они идут той же строкой — иначе экраны расходятся.
        val birthdays = dashboard.pair
            ?.birthdays()
            .orEmpty()
            .mapNotNull { birthday ->
            val days = Dates.daysUntil(birthday.upcomingDate(today), today)
            if (days in 0..limit) TodayEvent(birthday.name, days, isBirthday = true) else null
        }
        dashboard.copy(
            upcomingEvents = (events + birthdays).sortedWith(
                compareBy({ it.daysUntil }, { it.title })
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    fun savePair(
        nameA: String,
        nameB: String,
        sinceMillis: Long,
        birthdayA: Long?,
        birthdayB: Long?
    ) {
        val first = nameA.trim().ifEmpty { "Я" }
        val second = nameB.trim().ifEmpty { "Партнёр" }
        viewModelScope.launch {
            val existing = pairRepository.current()
            pairRepository.save(
                Pair(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    nameA = first,
                    nameB = second,
                    sinceMillis = sinceMillis,
                    birthdayA = birthdayA,
                    birthdayB = birthdayB
                )
            )
        }
    }
}
