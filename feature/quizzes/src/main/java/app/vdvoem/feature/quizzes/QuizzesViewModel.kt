package app.vdvoem.feature.quizzes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Итог прохождения: процент, баллы и трактовка. */
data class QuizOutcome(
    val quiz: Quiz,
    val score: Int,
    val maxScore: Int,
    val percent: Int,
    val interpretation: String,
    val passedAt: Long
)

data class QuizzesUiState(
    val results: Map<String, QuizResult> = emptyMap(),
    val activeQuiz: Quiz? = null,
    val questionIndex: Int = 0,
    val answers: List<Int> = emptyList(),
    val outcome: QuizOutcome? = null
)

/** Текущее прохождение — живёт только в памяти. */
private data class QuizSession(
    val quiz: Quiz? = null,
    val questionIndex: Int = 0,
    val answers: List<Int> = emptyList(),
    val outcome: QuizOutcome? = null
)

class QuizzesViewModel(
    private val resultsStore: QuizResultStore
) : ViewModel() {

    private val session = MutableStateFlow(QuizSession())

    val state: StateFlow<QuizzesUiState> =
        combine(resultsStore.results, session.asStateFlow()) { results, current ->
            QuizzesUiState(
                results = results,
                activeQuiz = current.quiz,
                questionIndex = current.questionIndex,
                answers = current.answers,
                outcome = current.outcome
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = QuizzesUiState()
        )

    fun start(quizId: String) {
        val quiz = QuizCatalog.byId(quizId) ?: return
        session.value = QuizSession(
            quiz = quiz,
            questionIndex = 0,
            answers = List(quiz.questions.size) { UNSELECTED }
        )
    }

    fun selectOption(optionIndex: Int) {
        val current = session.value
        val index = current.questionIndex
        if (optionIndex < 0 || optionIndex >= (current.quiz?.questions?.getOrNull(index)?.options?.size ?: 0)) return
        val answers = current.answers.toMutableList()
        answers[index] = optionIndex
        session.value = current.copy(answers = answers)
    }

    fun back() {
        val current = session.value
        if (current.questionIndex == 0) {
            close()
            return
        }
        session.value = current.copy(questionIndex = current.questionIndex - 1)
    }

    fun next() {
        val current = session.value
        val quiz = current.quiz ?: return
        val answered = current.answers.getOrNull(current.questionIndex) ?: UNSELECTED
        if (answered == UNSELECTED) return
        if (current.questionIndex < quiz.questions.lastIndex) {
            session.value = current.copy(questionIndex = current.questionIndex + 1)
        } else {
            finish(quiz, current.answers)
        }
    }

    fun retry() {
        val quiz = session.value.quiz ?: return
        start(quiz.id)
    }

    fun close() {
        session.value = QuizSession()
    }

    private fun finish(quiz: Quiz, answers: List<Int>) {
        val percent = quiz.percent(answers)
        val passedAt = System.currentTimeMillis()
        session.value = session.value.copy(
            outcome = QuizOutcome(
                quiz = quiz,
                score = quiz.score(answers),
                maxScore = quiz.maxScore,
                percent = percent,
                interpretation = quiz.interpretation(percent),
                passedAt = passedAt
            )
        )
        viewModelScope.launch {
            resultsStore.save(quizId = quiz.id, percent = percent, passedAt = passedAt)
        }
    }

    private companion object {
        const val UNSELECTED = -1
    }
}