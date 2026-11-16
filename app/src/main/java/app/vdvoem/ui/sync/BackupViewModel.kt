package app.vdvoem.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vdvoem.core.domain.repository.CalendarRepository
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

/**
 * Ручной обмен данными пары: выгрузить файл и загрузить его у партнёра.
 * Когда появится сервер, этот класс заменится сетевым синком.
 */
class BackupViewModel(
    private val pairRepository: PairRepository,
    private val calendarRepository: CalendarRepository,
    private val wishRepository: WishRepository,
    private val giftRepository: GiftRepository,
    private val pokeRepository: PokeRepository,
    private val quizScoreRepository: QuizScoreRepository,
    private val lessonRepository: LessonRepository
) : ViewModel() {

    suspend fun buildExport(): String = withContext(Dispatchers.IO) {
        PairBackup.export(
            PairBackup.Snapshot(
                pair = pairRepository.current(),
                events = calendarRepository.observeEvents().first(),
                wishes = wishRepository.observeWishes().first(),
                gifts = giftRepository.observeGifts().first(),
                pokes = pokeRepository.observe().first(),
                quizScores = quizScoreRepository.observe().first(),
                lessons = lessonRepository.observe().first()
            )
        )
    }

    suspend fun import(raw: String): ImportResult = withContext(Dispatchers.IO) {
        val snapshot = PairBackup.parse(raw) ?: return@withContext ImportResult(ok = false)

        snapshot.pair?.let { incoming ->
            if (pairRepository.current() == null) {
                pairRepository.save(incoming)
            }
        }

        snapshot.events.forEach { calendarRepository.upsert(it) }
        snapshot.wishes.forEach { wishRepository.upsert(it) }
        snapshot.gifts.forEach { giftRepository.upsert(it) }
        snapshot.pokes.forEach { pokeRepository.save(it) }
        snapshot.quizScores.forEach { quizScoreRepository.save(it) }
        snapshot.lessons.forEach { lessonRepository.save(it) }

        ImportResult(
            ok = true,
            imported = snapshot.total
        )
    }
}