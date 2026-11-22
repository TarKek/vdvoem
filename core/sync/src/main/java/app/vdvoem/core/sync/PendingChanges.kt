package app.vdvoem.core.sync

import app.vdvoem.core.data.LocalChangeSignal
import app.vdvoem.core.domain.repository.CalendarRepository
import app.vdvoem.core.domain.repository.CycleRepository
import app.vdvoem.core.domain.repository.GiftRepository
import app.vdvoem.core.domain.repository.LessonRepository
import app.vdvoem.core.domain.repository.PairRepository
import app.vdvoem.core.domain.repository.PokeRepository
import app.vdvoem.core.domain.repository.QuizScoreRepository
import app.vdvoem.core.domain.repository.TaskRepository
import app.vdvoem.core.domain.repository.WishRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * «Сколько локальных изменений ещё не уехало на сервер».
 *
 * Считаем ровно тем же правилом, что и [SyncEngine] при сборе исходящих изменений:
 * запись «не отправлена», если её `updatedAt` больше `SyncSettings.lastPushedAt`
 * (водяного знака последней успешной отправки). Отдельной очереди нет — это
 * первый шаг дорожной карты «офлайн-очередь», дальше сюда придёт настоящая очередь.
 *
 * Значение пересчитывается:
 * - при создании (инициализация);
 * - после каждого сигнала [LocalChangeSignal] (пользователь что-то изменил);
 * - после смены [SyncStatus] (проход синка завершился, `lastPushedAt` мог сдвинуться).
 *
 * Опрос по таймеру не используется: подписка на сигналы дешевле и точнее.
 */
class PendingChanges(
    private val settingsStore: SyncSettingsStore,
    private val pairRepository: PairRepository,
    private val calendarRepository: CalendarRepository,
    private val wishRepository: WishRepository,
    private val giftRepository: GiftRepository,
    private val pokeRepository: PokeRepository,
    private val quizScoreRepository: QuizScoreRepository,
    private val lessonRepository: LessonRepository,
    private val taskRepository: TaskRepository,
    private val cycleRepository: CycleRepository,
    private val statusHolder: SyncStatusHolder
) {

    private val _pending = MutableStateFlow(0)

    /** Текущее число неотправленных записей; UI читает как обычный `StateFlow`. */
    val pending: StateFlow<Int> = _pending.asStateFlow()

    private val mutex = Mutex()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            LocalChangeSignal.changes.collect { refresh() }
        }
        scope.launch {
            // Running — это начало прохода: пересчитывать рано, итог придёт в Success/Error.
            statusHolder.state.collect { status ->
                if (status !is SyncStatus.Running) refresh()
            }
        }
        scope.launch { refresh() }
    }

    /** Поток «не отправлено N» для UI: начинается с текущего значения. */
    fun observe(): Flow<Int> = pending

    /** Пересчитать и вернуть актуальное число неотправленных записей. */
    suspend fun count(): Int = refresh()

    /** Пересчёт с защитой от параллельных проходов (несколько сигналов подряд). */
    suspend fun refresh(): Int = mutex.withLock {
        val settings = runCatching { settingsStore.current() }.getOrNull()
        if (settings == null || !settings.isConfigured) {
            _pending.value = 0
            return@withLock 0
        }
        val pairId = settings.pairId
        val acknowledged = settingsStore.acknowledgements(pairId)
        val total = runCatching { countLocal(pairId, acknowledged) }.getOrElse { 0 }
        _pending.value = total
        total
    }

    private suspend fun countLocal(pairId: String, acknowledged: Map<String, Long>): Int {
        fun isPending(type: String, id: String, updatedAt: Long): Boolean =
            acknowledged[syncAckKey(pairId, type, id)] != updatedAt

        var total = 0
        if (pairRepository.current()?.let { it.id == pairId && isPending("pair", it.id, it.updatedAt) } == true) total++
        total += calendarRepository.observeEventsIncludingDeleted().first()
            .count { it.pairId == pairId && isPending("event", it.id, it.updatedAt) }
        total += wishRepository.observeWishesIncludingDeleted().first()
            .count { it.pairId == pairId && isPending("wish", it.id, it.updatedAt) }
        total += giftRepository.observeGiftsIncludingDeleted().first()
            .count { it.pairId == pairId && isPending("gift", it.id, it.updatedAt) }
        total += pokeRepository.observe().first()
            .count { it.pairId == pairId && isPending("poke", it.id, it.updatedAt) }
        total += quizScoreRepository.observe().first()
            .count { it.pairId == pairId && isPending("quiz", it.id, it.updatedAt) }
        total += lessonRepository.observeIncludingDeleted().first()
            .count { it.pairId == pairId && isPending("lesson", it.id, it.updatedAt) }
        total += taskRepository.observeIncludingDeleted().first()
            .count { it.pairId == pairId && isPending("task", it.id, it.updatedAt) }
        total += cycleRepository.observeDaysIncludingDeleted().first()
            .count { it.pairId == pairId && isPending("cycleDay", it.id, it.updatedAt) }
        total += cycleRepository.observeSettingsIncludingDeleted().first()
            .count { it.pairId == pairId && isPending("cycleSettings", it.id, it.updatedAt) }
        return total
    }
}