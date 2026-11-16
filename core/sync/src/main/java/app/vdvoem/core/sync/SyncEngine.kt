package app.vdvoem.core.sync

import app.vdvoem.core.data.LocalChangeSignal
import app.vdvoem.core.domain.model.CalendarEvent
import app.vdvoem.core.domain.model.CycleDay
import app.vdvoem.core.domain.model.CycleSettings
import app.vdvoem.core.domain.model.Gift
import app.vdvoem.core.domain.model.Lesson
import app.vdvoem.core.domain.model.Pair
import app.vdvoem.core.domain.model.Poke
import app.vdvoem.core.domain.model.QuizScore
import app.vdvoem.core.domain.model.Wish
import app.vdvoem.core.domain.repository.CalendarRepository
import app.vdvoem.core.domain.repository.CycleRepository
import app.vdvoem.core.domain.repository.GiftRepository
import app.vdvoem.core.domain.repository.LessonRepository
import app.vdvoem.core.domain.repository.PairRepository
import app.vdvoem.core.domain.repository.PokeRepository
import app.vdvoem.core.domain.repository.QuizScoreRepository
import app.vdvoem.core.domain.repository.WishRepository
import app.vdvoem.core.sync.SyncMappers.toDomain
import app.vdvoem.core.sync.SyncMappers.toDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Движок синхронизации: один проход = pull → применить (LWW) → push локальных изменений.
 *
 * Правила:
 * - `since` берётся из `lastServerTime` (серверное время), не из часов телефона;
 * - запись в репозиторий только если серверная запись новее локальной (LWW по `updatedAt`);
 * - локальные изменения определяются как `updatedAt > lastServerTime` — без отдельной очереди;
 * - после успеха `lastServerTime` обновляется, повторный проход не отправляет то же самое.
 *
 * Проходы сериализованы [mutex]: параллельные вызовы (воркер и кнопка «Синхронизировать сейчас»)
 * не дублируют запросы.
 */
class SyncEngine(
    private val settingsStore: SyncSettingsStore,
    private val api: SyncApi,
    private val pairRepository: PairRepository,
    private val calendarRepository: CalendarRepository,
    private val wishRepository: WishRepository,
    private val giftRepository: GiftRepository,
    private val pokeRepository: PokeRepository,
    private val quizScoreRepository: QuizScoreRepository,
    private val lessonRepository: LessonRepository,
    private val cycleRepository: CycleRepository,
    private val statusHolder: SyncStatusHolder,
    /** Колбэк «пришло новое сердечко от партнёра»: уведомление рисует слой приложения. */
    private val pokeReceiver: suspend (List<Poke>) -> Unit = {}
) {

    private val mutex = Mutex()

    private val _lastResult = MutableStateFlow<SyncResult?>(null)
    val lastResult: StateFlow<SyncResult?> = _lastResult.asStateFlow()

    suspend fun syncOnce(): SyncResult = mutex.withLock {
        val settings = settingsStore.current()
        if (!settings.isConfigured) {
            statusHolder.set(SyncStatus.Error(NOT_CONFIGURED))
            return@withLock SyncResult.NotConfigured.also { _lastResult.value = it }
        }

        statusHolder.set(SyncStatus.Running)
        val since = settings.lastServerTime
        val pairId = settings.pairId

        try {
            LocalChangeSignal.muted {
            var pulled = 0
            var applied = 0

            val remote = api.pull(since)
            pulled += remote.size

            // Снимок локальных изменений до применения серверных: так только что
            // применённые записи не уезжают обратно на сервер «эхом».
            val outgoing = collectLocalChanges(settings.lastPushedAt)
            val incomingPokes = mutableListOf<Poke>()

            applied += applyPair(remote.pair)
            applied += applyEvents(remote.events, pairId)
            applied += applyWishes(remote.wishes, pairId)
            applied += applyGifts(remote.gifts, pairId)
            remote.pokes.forEach { poke ->
                if (applyPoke(poke, pairId)) incomingPokes += poke.toDomain(fallbackPairId = pairId)
            }
            if (incomingPokes.isNotEmpty()) pokeReceiver(incomingPokes)

            applied += applyQuizScores(remote.quizScores, pairId)
            applied += applyLessons(remote.lessons, pairId)
            applied += applyCycleDays(remote.cycleDays, pairId)
            applied += applyCycleSettings(remote.cycleSettings, pairId)

            var pushed = 0
            if (!outgoing.isEmpty) {
                pushed = outgoing.size
                val response = api.push(outgoing)
                pulled += response.size
                applied += applyPair(response.pair)
                applied += applyEvents(response.events, pairId)
                applied += applyWishes(response.wishes, pairId)
                applied += applyGifts(response.gifts, pairId)
                response.pokes.forEach { poke ->
                    if (applyPoke(poke, pairId)) incomingPokes += poke.toDomain(fallbackPairId = pairId)
                }
                if (incomingPokes.isNotEmpty()) pokeReceiver(incomingPokes)

                applied += applyQuizScores(response.quizScores, pairId)
                applied += applyLessons(response.lessons, pairId)
                applied += applyCycleDays(response.cycleDays, pairId)
                applied += applyCycleSettings(response.cycleSettings, pairId)

                // Отмечаем, что именно уже уехало: следующая отправка считается
                // не от серверного времени, а от локального «водяного знака».
                val pushedMax = outgoing.maxUpdatedAt()
                if (pushedMax > settings.lastPushedAt) settingsStore.setLastPushedAt(pushedMax)
            }

            val serverTime = maxOf(remote.serverTime, settings.lastServerTime)
            settingsStore.setLastServerTime(serverTime)

            val result = SyncResult.Success(pulled = pulled, applied = applied, serverTime = serverTime)
            _lastResult.value = result
            statusHolder.set(SyncStatus.Success(serverTime))
            android.util.Log.d(TAG, "sync ok: pulled=$pulled applied=$applied pushed=$pushed since=$since")
            result
            }
        } catch (cancelled: CancellationException) {
            statusHolder.set(SyncStatus.Idle)
            throw cancelled
        } catch (error: Exception) {
            val message = error.message ?: "Ошибка синхронизации"
            val result = SyncResult.Error(message = message, cause = error)
            _lastResult.value = result
            statusHolder.set(SyncStatus.Error(message))
            android.util.Log.w(TAG, "sync failed: $message")
            result
        }
    }

    // --- применение серверных изменений (LWW) ---

    private suspend fun applyPair(remote: PairDto?): Int {
        if (remote == null) return 0
        val local = pairRepository.current()
        if (local == null || remote.updatedAt > local.updatedAt) {
            pairRepository.save(remote.toDomain())
            return 1
        }
        return 0
    }

    private suspend fun applyEvents(remote: List<EventDto>, pairId: String): Int {
        if (remote.isEmpty()) return 0
        val localById = calendarRepository.observeEvents().first().associateBy { it.id }
        var applied = 0
        remote.forEach { dto ->
            val local = localById[dto.id]
            if (local == null || dto.updatedAt > local.updatedAt) {
                calendarRepository.upsert(dto.toDomain(fallbackPairId = pairId))
                applied++
            }
        }
        return applied
    }

    private suspend fun applyWishes(remote: List<WishDto>, pairId: String): Int {
        if (remote.isEmpty()) return 0
        // Tombstone тоже читаем: иначе удалённое желание «оживало» бы от старой копии.
        val localById = wishRepository.observeWishesIncludingDeleted().first().associateBy { it.id }
        var applied = 0
        remote.forEach { dto ->
            val local = localById[dto.id]
            if (shouldApplyRemote(local?.updatedAt, local?.deletedAt, dto.updatedAt)) {
                wishRepository.upsert(dto.toDomain(fallbackPairId = pairId))
                applied++
            }
        }
        return applied
    }

    private suspend fun applyGifts(remote: List<GiftDto>, pairId: String): Int {
        if (remote.isEmpty()) return 0
        val localById = giftRepository.observeGiftsIncludingDeleted().first().associateBy { it.id }
        var applied = 0
        remote.forEach { dto ->
            val local = localById[dto.id]
            if (shouldApplyRemote(local?.updatedAt, local?.deletedAt, dto.updatedAt)) {
                giftRepository.upsert(dto.toDomain(fallbackPairId = pairId))
                applied++
            }
        }
        return applied
    }

    /** true — сердечко реально новое (нужно показать уведомление). */
    private suspend fun applyPoke(remote: PokeDto, pairId: String): Boolean {
        val localById = pokeRepository.observe().first().associateBy { it.id }
        val local = localById[remote.id]
        if (local != null && remote.updatedAt <= local.updatedAt) return false
        pokeRepository.save(remote.toDomain(fallbackPairId = pairId))
        return true
    }

    /** Применяет результаты тестов партнёра; возвращает число новых записей. */
    private suspend fun applyQuizScores(remote: List<QuizScoreDto>, pairId: String): Int {
        if (remote.isEmpty()) return 0
        val localById = quizScoreRepository.observe().first().associateBy { it.id }
        var applied = 0
        remote.forEach { dto ->
            val local = localById[dto.id]
            if (local == null || dto.updatedAt > local.updatedAt) {
                quizScoreRepository.save(dto.toDomain(fallbackPairId = pairId))
                applied++
            }
        }
        return applied
    }

    /** Применяет уроки партнёра; возвращает число новых записей. */
    private suspend fun applyLessons(remote: List<LessonDto>, pairId: String): Int {
        if (remote.isEmpty()) return 0
        val localById = lessonRepository.observe().first().associateBy { it.id }
        var applied = 0
        remote.forEach { dto ->
            val local = localById[dto.id]
            if (local == null || dto.updatedAt > local.updatedAt) {
                lessonRepository.save(dto.toDomain(fallbackPairId = pairId))
                applied++
            }
        }
        return applied
    }

    /** Применяет дни цикла партнёра; tombstone важнее старых локальных копий. */
    private suspend fun applyCycleDays(remote: List<CycleDayDto>, pairId: String): Int {
        if (remote.isEmpty()) return 0
        val localById = cycleRepository.observeDaysIncludingDeleted().first().associateBy { it.id }
        var applied = 0
        remote.forEach { dto ->
            val local = localById[dto.id]
            if (shouldApplyRemote(local?.updatedAt, local?.deletedAt, dto.updatedAt)) {
                cycleRepository.saveDay(dto.toDomain(fallbackPairId = pairId))
                applied++
            }
        }
        return applied
    }

    /** Применяет настройки цикла пары; запись одна, конфликт решается по `updatedAt`. */
    private suspend fun applyCycleSettings(remote: List<CycleSettingsDto>, pairId: String): Int {
        if (remote.isEmpty()) return 0
        val localById = cycleRepository.observeSettingsIncludingDeleted().first().associateBy { it.id }
        var applied = 0
        remote.forEach { dto ->
            val local = localById[dto.id]
            if (shouldApplyRemote(local?.updatedAt, local?.deletedAt, dto.updatedAt)) {
                cycleRepository.saveSettings(dto.toDomain(fallbackPairId = pairId))
                applied++
            }
        }
        return applied
    }

    // --- сбор локальных изменений ---

    private suspend fun collectLocalChanges(pushedBefore: Long): ChangesDto {
        val localPair: Pair? = pairRepository.current()?.takeIf { it.updatedAt > pushedBefore }
        val events: List<CalendarEvent> = calendarRepository.observeEvents().first()
            .filter { it.updatedAt > pushedBefore }
        val wishes: List<Wish> = wishRepository.observeWishesIncludingDeleted().first()
            .filter { it.updatedAt > pushedBefore }
        val gifts: List<Gift> = giftRepository.observeGiftsIncludingDeleted().first()
            .filter { it.updatedAt > pushedBefore }
        val pokes: List<Poke> = pokeRepository.observe().first()
            .filter { it.updatedAt > pushedBefore }
        val quizScores: List<QuizScore> = quizScoreRepository.observe().first()
            .filter { it.updatedAt > pushedBefore }
        val lessons: List<Lesson> = lessonRepository.observe().first()
            .filter { it.updatedAt > pushedBefore }
        val cycleDays: List<CycleDay> = cycleRepository.observeDaysIncludingDeleted().first()
            .filter { it.updatedAt > pushedBefore }
        val cycleSettings: List<CycleSettings> = cycleRepository.observeSettingsIncludingDeleted().first()
            .filter { it.updatedAt > pushedBefore }

        return ChangesDto(
            pair = localPair?.toDto(),
            events = events.map { it.toDto() },
            wishes = wishes.map { it.toDto() },
            gifts = gifts.map { it.toDto() },
            pokes = pokes.map { it.toDto() },
            quizScores = quizScores.map { it.toDto() },
            lessons = lessons.map { it.toDto() },
            cycleDays = cycleDays.map { it.toDto() },
            cycleSettings = cycleSettings.map { it.toDto() }
        )
    }

    private companion object {
        const val TAG = "SyncEngine"
        const val NOT_CONFIGURED = "Синхронизация не настроена"
    }
}