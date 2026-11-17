package app.vdvoem.ui.sync

import androidx.lifecycle.ViewModel
import androidx.room.withTransaction
import app.vdvoem.core.data.db.VdvoemDatabase
import app.vdvoem.core.domain.device.DeviceIdentity
import app.vdvoem.core.domain.model.GiftStatus
import app.vdvoem.core.domain.repository.CalendarRepository
import app.vdvoem.core.domain.repository.CycleRepository
import app.vdvoem.core.domain.repository.GiftRepository
import app.vdvoem.core.domain.repository.LessonRepository
import app.vdvoem.core.domain.repository.PairRepository
import app.vdvoem.core.domain.repository.PokeRepository
import app.vdvoem.core.domain.repository.QuizScoreRepository
import app.vdvoem.core.domain.repository.WishRepository
import app.vdvoem.sync.PairBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class ImportResult(
    val ok: Boolean,
    val imported: Int = 0
)

/** Атомарный LWW-импорт и приватный экспорт ручной копии пары. */
class BackupViewModel(
    private val pairRepository: PairRepository,
    private val calendarRepository: CalendarRepository,
    private val wishRepository: WishRepository,
    private val giftRepository: GiftRepository,
    private val pokeRepository: PokeRepository,
    private val quizScoreRepository: QuizScoreRepository,
    private val lessonRepository: LessonRepository,
    private val cycleRepository: CycleRepository,
    private val deviceIdentity: DeviceIdentity,
    private val database: VdvoemDatabase
) : ViewModel() {

    suspend fun buildExport(): String = withContext(Dispatchers.IO) {
        val myDevice = deviceIdentity.deviceId()
        PairBackup.export(
            PairBackup.Snapshot(
                pair = pairRepository.current(),
                events = calendarRepository.observeEvents().first(),
                wishes = wishRepository.observeWishes().first().filter {
                    !it.surprise || it.ownerDeviceId == myDevice
                },
                gifts = giftRepository.observeGifts().first().filter {
                    !it.surprise || it.status == GiftStatus.GIVEN || it.ownerDeviceId == myDevice
                },
                pokes = pokeRepository.observe().first(),
                quizScores = quizScoreRepository.observe().first(),
                lessons = lessonRepository.observe().first(),
                // Ручная копия не содержит приватный payload удалённых медицинских записей.
                cycleDays = cycleRepository.observeDays().first(),
                cycleSettings = cycleRepository.observeSettings().first()
            )
        )
    }

    suspend fun import(raw: String): ImportResult = withContext(Dispatchers.IO) {
        val snapshot = PairBackup.parse(raw) ?: return@withContext ImportResult(ok = false)
        if (!PairBackup.hasValidIds(snapshot)) return@withContext ImportResult(ok = false)
        val myDevice = deviceIdentity.deviceId()
        var imported = 0

        database.withTransaction {
            snapshot.pair?.let { incoming ->
                val local = pairRepository.current()
                if (local == null || isNewer(incoming.updatedAt, null, local.updatedAt, null)) {
                    pairRepository.save(incoming)
                    imported++
                }
            }

            val events = calendarRepository.observeEventsIncludingDeleted().first().associateBy { it.id }
            snapshot.events.forEach { incoming ->
                val local = events[incoming.id]
                if (isNewer(incoming.updatedAt, incoming.deletedAt, local?.updatedAt, local?.deletedAt)) {
                    calendarRepository.upsert(incoming)
                    imported++
                }
            }

            val wishes = wishRepository.observeWishesIncludingDeleted().first().associateBy { it.id }
            snapshot.wishes.filter { !it.surprise || it.ownerDeviceId == myDevice }.forEach { incoming ->
                val local = wishes[incoming.id]
                if (isNewer(incoming.updatedAt, incoming.deletedAt, local?.updatedAt, local?.deletedAt)) {
                    wishRepository.upsert(incoming)
                    imported++
                }
            }

            val gifts = giftRepository.observeGiftsIncludingDeleted().first().associateBy { it.id }
            snapshot.gifts.filter {
                !it.surprise || it.status == GiftStatus.GIVEN || it.ownerDeviceId == myDevice
            }.forEach { incoming ->
                val local = gifts[incoming.id]
                if (isNewer(incoming.updatedAt, incoming.deletedAt, local?.updatedAt, local?.deletedAt)) {
                    giftRepository.upsert(incoming)
                    imported++
                }
            }

            val pokes = pokeRepository.observeIncludingDeleted().first().associateBy { it.id }
            snapshot.pokes.forEach { incoming ->
                val local = pokes[incoming.id]
                if (isNewer(incoming.updatedAt, incoming.deletedAt, local?.updatedAt, local?.deletedAt)) {
                    pokeRepository.save(incoming)
                    imported++
                }
            }

            val scores = quizScoreRepository.observeIncludingDeleted().first().associateBy { it.id }
            snapshot.quizScores.forEach { incoming ->
                val local = scores[incoming.id]
                if (isNewer(incoming.updatedAt, incoming.deletedAt, local?.updatedAt, local?.deletedAt)) {
                    quizScoreRepository.save(incoming)
                    imported++
                }
            }

            val lessons = lessonRepository.observeIncludingDeleted().first().associateBy { it.id }
            snapshot.lessons.forEach { incoming ->
                val local = lessons[incoming.id]
                if (isNewer(incoming.updatedAt, incoming.deletedAt, local?.updatedAt, local?.deletedAt)) {
                    lessonRepository.save(incoming)
                    imported++
                }
            }

            val days = cycleRepository.observeDaysIncludingDeleted().first().associateBy { it.id }
            snapshot.cycleDays.forEach { incoming ->
                val local = days[incoming.id]
                if (isNewer(incoming.updatedAt, incoming.deletedAt, local?.updatedAt, local?.deletedAt)) {
                    cycleRepository.saveDay(incoming)
                    imported++
                }
            }

            snapshot.cycleSettings?.let { incoming ->
                val local = cycleRepository.observeSettingsIncludingDeleted().first()
                    .firstOrNull { it.id == incoming.id }
                if (isNewer(incoming.updatedAt, incoming.deletedAt, local?.updatedAt, local?.deletedAt)) {
                    cycleRepository.saveSettings(incoming)
                    imported++
                }
            }
        }
        ImportResult(ok = true, imported = imported)
    }

    private fun isNewer(remoteAt: Long, remoteDeleted: Long?, localAt: Long?, localDeleted: Long?): Boolean = when {
        localAt == null || remoteAt > localAt -> true
        remoteAt < localAt -> false
        else -> remoteDeleted != null && localDeleted == null
    }
}
