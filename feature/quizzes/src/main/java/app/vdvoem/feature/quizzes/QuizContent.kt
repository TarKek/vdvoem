package app.vdvoem.feature.quizzes

import androidx.annotation.StringRes
import kotlin.math.roundToInt

/**
 * Вариант ответа: текст-ресурс и вес в баллах.
 *
 * Вес от 0 до 3 — чем больше балл, тем «здоровее» ответ для пары.
 */
data class QuizOption(
    @StringRes val textRes: Int,
    val score: Int
)

/** Вопрос теста и его варианты (2–4 штуки). */
data class QuizQuestion(
    @StringRes val textRes: Int,
    val options: List<QuizOption>
)

/**
 * Тест целиком: тексты лежат в ресурсах модуля (`values/strings.xml` и `values-en/strings.xml`),
 * а в коде остаются только идентификаторы, баллы и структура.
 *
 * [interpretations] — ровно три трактовки результата: низкая, средняя и высокая.
 */
data class Quiz(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val questions: List<QuizQuestion>,
    @StringRes val interpretations: List<Int>
) {

    /** Максимум баллов, который можно набрать. */
    val maxScore: Int
        get() = questions.sumOf { question -> question.options.maxOfOrNull { it.score } ?: 0 }

    /** Баллы за набор ответов; незаполненные вопросы дают 0. */
    fun score(answers: List<Int>): Int =
        questions.mapIndexed { index, question ->
            val chosen = answers.getOrNull(index) ?: -1
            question.options.getOrNull(chosen)?.score ?: 0
        }.sum()

    /** Результат 0..100. */
    fun percent(answers: List<Int>): Int {
        val max = maxScore
        if (max <= 0) return 0
        return (score(answers) * 100.0 / max).roundToInt().coerceIn(0, 100)
    }

    /** Короткая интерпретация по проценту: три градации. */
    @StringRes
    fun interpretationRes(percent: Int): Int =
        interpretations.getOrElse(levelOf(percent)) { interpretations.last() }

    companion object {
        /** 0 — низкая (< 40 %), 1 — средняя (40–69 %), 2 — высокая (70 %+). */
        fun levelOf(percent: Int): Int = when {
            percent < LOW_BOUND -> 0
            percent < HIGH_BOUND -> 1
            else -> 2
        }

        private const val LOW_BOUND = 40
        private const val HIGH_BOUND = 70
    }
}

/** Каталог тестов: без интернета, все данные в APK. */
object QuizCatalog {

    const val COMPATIBILITY_ID = "compatibility"
    const val LOVE_LANGUAGE_ID = "love_language"
    const val WHO_IS_WHO_ID = "who_is_who"
    const val HOME_MONEY_ID = "home_money"
    const val FUTURE_PLANS_ID = "future_plans"

    val quizzes: List<Quiz> = listOf(
        compatibilityQuiz(),
        loveLanguageQuiz(),
        whoIsWhoQuiz(),
        homeMoneyQuiz(),
        futurePlansQuiz()
    )

    fun byId(id: String): Quiz? = quizzes.firstOrNull { it.id == id }

    private fun compatibilityQuiz() = Quiz(
        id = COMPATIBILITY_ID,
        titleRes = R.string.quiz_compat_title,
        descriptionRes = R.string.quiz_compat_description,
        interpretations = listOf(
            R.string.quiz_compat_result_low,
            R.string.quiz_compat_result_mid,
            R.string.quiz_compat_result_high
        ),
        questions = listOf(
            QuizQuestion(
                textRes = R.string.quiz_compat_q1,
                options = listOf(
                    QuizOption(R.string.quiz_compat_q1_a1, 3),
                    QuizOption(R.string.quiz_compat_q1_a2, 2),
                    QuizOption(R.string.quiz_compat_q1_a3, 1),
                    QuizOption(R.string.quiz_compat_q1_a4, 0)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_compat_q2,
                options = listOf(
                    QuizOption(R.string.quiz_compat_q2_a1, 3),
                    QuizOption(R.string.quiz_compat_q2_a2, 2),
                    QuizOption(R.string.quiz_compat_q2_a3, 1)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_compat_q3,
                options = listOf(
                    QuizOption(R.string.quiz_compat_q3_a1, 2),
                    QuizOption(R.string.quiz_compat_q3_a2, 2),
                    QuizOption(R.string.quiz_compat_q3_a3, 0),
                    QuizOption(R.string.quiz_compat_q3_a4, 1)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_compat_q4,
                options = listOf(
                    QuizOption(R.string.quiz_compat_q4_a1, 3),
                    QuizOption(R.string.quiz_compat_q4_a2, 2),
                    QuizOption(R.string.quiz_compat_q4_a3, 0),
                    QuizOption(R.string.quiz_compat_q4_a4, 1)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_compat_q5,
                options = listOf(
                    QuizOption(R.string.quiz_compat_q5_a1, 3),
                    QuizOption(R.string.quiz_compat_q5_a2, 2),
                    QuizOption(R.string.quiz_compat_q5_a3, 1),
                    QuizOption(R.string.quiz_compat_q5_a4, 0)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_compat_q6,
                options = listOf(
                    QuizOption(R.string.quiz_compat_q6_a1, 3),
                    QuizOption(R.string.quiz_compat_q6_a2, 2),
                    QuizOption(R.string.quiz_compat_q6_a3, 1)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_compat_q7,
                options = listOf(
                    QuizOption(R.string.quiz_compat_q7_a1, 3),
                    QuizOption(R.string.quiz_compat_q7_a2, 2),
                    QuizOption(R.string.quiz_compat_q7_a3, 0),
                    QuizOption(R.string.quiz_compat_q7_a4, 1)
                )
            )
        )
    )

    private fun loveLanguageQuiz() = Quiz(
        id = LOVE_LANGUAGE_ID,
        titleRes = R.string.quiz_love_title,
        descriptionRes = R.string.quiz_love_description,
        interpretations = listOf(
            R.string.quiz_love_result_low,
            R.string.quiz_love_result_mid,
            R.string.quiz_love_result_high
        ),
        questions = listOf(
            QuizQuestion(
                textRes = R.string.quiz_love_q1,
                options = listOf(
                    QuizOption(R.string.quiz_love_q1_a1, 3),
                    QuizOption(R.string.quiz_love_q1_a2, 2),
                    QuizOption(R.string.quiz_love_q1_a3, 1),
                    QuizOption(R.string.quiz_love_q1_a4, 0)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_love_q2,
                options = listOf(
                    QuizOption(R.string.quiz_love_q2_a1, 3),
                    QuizOption(R.string.quiz_love_q2_a2, 2),
                    QuizOption(R.string.quiz_love_q2_a3, 1),
                    QuizOption(R.string.quiz_love_q2_a4, 0)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_love_q3,
                options = listOf(
                    QuizOption(R.string.quiz_love_q3_a1, 3),
                    QuizOption(R.string.quiz_love_q3_a2, 2),
                    QuizOption(R.string.quiz_love_q3_a3, 1),
                    QuizOption(R.string.quiz_love_q3_a4, 0)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_love_q4,
                options = listOf(
                    QuizOption(R.string.quiz_love_q4_a1, 3),
                    QuizOption(R.string.quiz_love_q4_a2, 2),
                    QuizOption(R.string.quiz_love_q4_a3, 1),
                    QuizOption(R.string.quiz_love_q4_a4, 0)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_love_q5,
                options = listOf(
                    QuizOption(R.string.quiz_love_q5_a1, 3),
                    QuizOption(R.string.quiz_love_q5_a2, 2),
                    QuizOption(R.string.quiz_love_q5_a3, 1),
                    QuizOption(R.string.quiz_love_q5_a4, 0)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_love_q6,
                options = listOf(
                    QuizOption(R.string.quiz_love_q6_a1, 3),
                    QuizOption(R.string.quiz_love_q6_a2, 2),
                    QuizOption(R.string.quiz_love_q6_a3, 1),
                    QuizOption(R.string.quiz_love_q6_a4, 0)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_love_q7,
                options = listOf(
                    QuizOption(R.string.quiz_love_q7_a1, 3),
                    QuizOption(R.string.quiz_love_q7_a2, 2),
                    QuizOption(R.string.quiz_love_q7_a3, 1),
                    QuizOption(R.string.quiz_love_q7_a4, 0)
                )
            )
        )
    )

    private fun whoIsWhoQuiz() = Quiz(
        id = WHO_IS_WHO_ID,
        titleRes = R.string.quiz_who_title,
        descriptionRes = R.string.quiz_who_description,
        interpretations = listOf(
            R.string.quiz_who_result_low,
            R.string.quiz_who_result_mid,
            R.string.quiz_who_result_high
        ),
        questions = listOf(
            QuizQuestion(
                textRes = R.string.quiz_who_q1,
                options = listOf(
                    QuizOption(R.string.quiz_who_q1_a1, 3),
                    QuizOption(R.string.quiz_who_q1_a2, 2),
                    QuizOption(R.string.quiz_who_q1_a3, 2),
                    QuizOption(R.string.quiz_who_q1_a4, 0)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_who_q2,
                options = listOf(
                    QuizOption(R.string.quiz_who_q2_a1, 3),
                    QuizOption(R.string.quiz_who_q2_a2, 2),
                    QuizOption(R.string.quiz_who_q2_a3, 2),
                    QuizOption(R.string.quiz_who_q2_a4, 1)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_who_q3,
                options = listOf(
                    QuizOption(R.string.quiz_who_q3_a1, 3),
                    QuizOption(R.string.quiz_who_q3_a2, 2),
                    QuizOption(R.string.quiz_who_q3_a3, 2),
                    QuizOption(R.string.quiz_who_q3_a4, 1)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_who_q4,
                options = listOf(
                    QuizOption(R.string.quiz_who_q4_a1, 3),
                    QuizOption(R.string.quiz_who_q4_a2, 2),
                    QuizOption(R.string.quiz_who_q4_a3, 2),
                    QuizOption(R.string.quiz_who_q4_a4, 0)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_who_q5,
                options = listOf(
                    QuizOption(R.string.quiz_who_q5_a1, 3),
                    QuizOption(R.string.quiz_who_q5_a2, 2),
                    QuizOption(R.string.quiz_who_q5_a3, 2),
                    QuizOption(R.string.quiz_who_q5_a4, 0)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_who_q6,
                options = listOf(
                    QuizOption(R.string.quiz_who_q6_a1, 3),
                    QuizOption(R.string.quiz_who_q6_a2, 2),
                    QuizOption(R.string.quiz_who_q6_a3, 2),
                    QuizOption(R.string.quiz_who_q6_a4, 0)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_who_q7,
                options = listOf(
                    QuizOption(R.string.quiz_who_q7_a1, 3),
                    QuizOption(R.string.quiz_who_q7_a2, 2),
                    QuizOption(R.string.quiz_who_q7_a3, 2),
                    QuizOption(R.string.quiz_who_q7_a4, 1)
                )
            )
        )
    )

    private fun homeMoneyQuiz() = Quiz(
        id = HOME_MONEY_ID,
        titleRes = R.string.quiz_home_title,
        descriptionRes = R.string.quiz_home_description,
        interpretations = listOf(
            R.string.quiz_home_result_low,
            R.string.quiz_home_result_mid,
            R.string.quiz_home_result_high
        ),
        questions = listOf(
            QuizQuestion(
                textRes = R.string.quiz_home_q1,
                options = listOf(
                    QuizOption(R.string.quiz_home_q1_a1, 3),
                    QuizOption(R.string.quiz_home_q1_a2, 2),
                    QuizOption(R.string.quiz_home_q1_a3, 1),
                    QuizOption(R.string.quiz_home_q1_a4, 0)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_home_q2,
                options = listOf(
                    QuizOption(R.string.quiz_home_q2_a1, 3),
                    QuizOption(R.string.quiz_home_q2_a2, 2),
                    QuizOption(R.string.quiz_home_q2_a3, 1),
                    QuizOption(R.string.quiz_home_q2_a4, 0)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_home_q3,
                options = listOf(
                    QuizOption(R.string.quiz_home_q3_a1, 3),
                    QuizOption(R.string.quiz_home_q3_a2, 2),
                    QuizOption(R.string.quiz_home_q3_a3, 1),
                    QuizOption(R.string.quiz_home_q3_a4, 0)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_home_q4,
                options = listOf(
                    QuizOption(R.string.quiz_home_q4_a1, 3),
                    QuizOption(R.string.quiz_home_q4_a2, 2),
                    QuizOption(R.string.quiz_home_q4_a3, 1),
                    QuizOption(R.string.quiz_home_q4_a4, 0)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_home_q5,
                options = listOf(
                    QuizOption(R.string.quiz_home_q5_a1, 3),
                    QuizOption(R.string.quiz_home_q5_a2, 2),
                    QuizOption(R.string.quiz_home_q5_a3, 1),
                    QuizOption(R.string.quiz_home_q5_a4, 0)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_home_q6,
                options = listOf(
                    QuizOption(R.string.quiz_home_q6_a1, 3),
                    QuizOption(R.string.quiz_home_q6_a2, 2),
                    QuizOption(R.string.quiz_home_q6_a3, 1),
                    QuizOption(R.string.quiz_home_q6_a4, 0)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_home_q7,
                options = listOf(
                    QuizOption(R.string.quiz_home_q7_a1, 3),
                    QuizOption(R.string.quiz_home_q7_a2, 2),
                    QuizOption(R.string.quiz_home_q7_a3, 1),
                    QuizOption(R.string.quiz_home_q7_a4, 0)
                )
            )
        )
    )

    private fun futurePlansQuiz() = Quiz(
        id = FUTURE_PLANS_ID,
        titleRes = R.string.quiz_future_title,
        descriptionRes = R.string.quiz_future_description,
        interpretations = listOf(
            R.string.quiz_future_result_low,
            R.string.quiz_future_result_mid,
            R.string.quiz_future_result_high
        ),
        questions = listOf(
            QuizQuestion(
                textRes = R.string.quiz_future_q1,
                options = listOf(
                    QuizOption(R.string.quiz_future_q1_a1, 3),
                    QuizOption(R.string.quiz_future_q1_a2, 2),
                    QuizOption(R.string.quiz_future_q1_a3, 1),
                    QuizOption(R.string.quiz_future_q1_a4, 0)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_future_q2,
                options = listOf(
                    QuizOption(R.string.quiz_future_q2_a1, 3),
                    QuizOption(R.string.quiz_future_q2_a2, 2),
                    QuizOption(R.string.quiz_future_q2_a3, 1),
                    QuizOption(R.string.quiz_future_q2_a4, 0)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_future_q3,
                options = listOf(
                    QuizOption(R.string.quiz_future_q3_a1, 3),
                    QuizOption(R.string.quiz_future_q3_a2, 2),
                    QuizOption(R.string.quiz_future_q3_a3, 1),
                    QuizOption(R.string.quiz_future_q3_a4, 0)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_future_q4,
                options = listOf(
                    QuizOption(R.string.quiz_future_q4_a1, 3),
                    QuizOption(R.string.quiz_future_q4_a2, 2),
                    QuizOption(R.string.quiz_future_q4_a3, 1),
                    QuizOption(R.string.quiz_future_q4_a4, 0)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_future_q5,
                options = listOf(
                    QuizOption(R.string.quiz_future_q5_a1, 3),
                    QuizOption(R.string.quiz_future_q5_a2, 2),
                    QuizOption(R.string.quiz_future_q5_a3, 1),
                    QuizOption(R.string.quiz_future_q5_a4, 0)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_future_q6,
                options = listOf(
                    QuizOption(R.string.quiz_future_q6_a1, 3),
                    QuizOption(R.string.quiz_future_q6_a2, 2),
                    QuizOption(R.string.quiz_future_q6_a3, 1),
                    QuizOption(R.string.quiz_future_q6_a4, 0)
                )
            ),
            QuizQuestion(
                textRes = R.string.quiz_future_q7,
                options = listOf(
                    QuizOption(R.string.quiz_future_q7_a1, 3),
                    QuizOption(R.string.quiz_future_q7_a2, 2),
                    QuizOption(R.string.quiz_future_q7_a3, 1),
                    QuizOption(R.string.quiz_future_q7_a4, 0)
                )
            )
        )
    )
}