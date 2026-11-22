package app.vdvoem.feature.timetable

import app.vdvoem.core.domain.model.Lesson
import app.vdvoem.core.domain.model.Pair
import app.vdvoem.core.domain.repository.LessonRepository
import app.vdvoem.core.domain.repository.PairRepository
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class TimetableViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `add lesson immediately uses the newly selected date`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        runCurrent()

        // 27 ноября 2026 — пятница. Раньше addLesson читал ещё не обновлённый state
        // и мог открыть черновик на предыдущем дне недели.
        viewModel.selectDate(LocalDate.of(2026, 11, 27))
        viewModel.addLesson()
        runCurrent()

        assertEquals(5, viewModel.state.value.draft?.weekday)
        assertEquals(LocalDate.of(2026, 11, 27), viewModel.state.value.selectedDate)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `refresh today moves the day but keeps the selected date`() = runTest(dispatcher) {
        var now = LocalDate.of(2026, 11, 22)
        val viewModel = createViewModel { now }
        runCurrent()

        val selected = LocalDate.of(2026, 11, 23)
        viewModel.selectDate(selected)
        runCurrent()

        now = LocalDate.of(2026, 11, 23)
        viewModel.refreshToday()
        runCurrent()

        assertEquals(now, viewModel.state.value.today)
        assertEquals(selected, viewModel.state.value.selectedDate)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `millis until next midnight includes a one second safety margin`() {
        assertEquals(
            43_201_000L,
            millisUntilNextMidnight(LocalDateTime.of(2026, 11, 22, 12, 0))
        )
        assertEquals(
            2_000L,
            millisUntilNextMidnight(LocalDateTime.of(2026, 11, 22, 23, 59, 59))
        )
    }

    private fun createViewModel(
        todayProvider: () -> LocalDate = { LocalDate.of(2026, 11, 22) }
    ) = TimetableViewModel(
        repository = FakeLessonRepository(),
        pairRepository = FakePairRepository(),
        todayProvider = todayProvider
    )

    private class FakeLessonRepository : LessonRepository {
        private val lessons = MutableStateFlow<List<Lesson>>(emptyList())

        override fun observe(): Flow<List<Lesson>> = lessons
        override fun observeIncludingDeleted(): Flow<List<Lesson>> = lessons
        override suspend fun save(lesson: Lesson) = Unit
        override suspend fun delete(lesson: Lesson) = Unit
    }

    private class FakePairRepository : PairRepository {
        private val pair = MutableStateFlow<Pair?>(null)

        override fun observe(): Flow<Pair?> = pair
        override suspend fun save(pair: Pair) = Unit
        override suspend fun current(): Pair? = null
    }
}