package app.vdvoem.feature.quizzes

import kotlin.math.roundToInt

/**
 * Вариант ответа: текст и вес в баллах.
 *
 * Вес от 0 до 3 — чем больше балл, тем «здоровее» ответ для пары.
 */
data class QuizOption(
    val text: String,
    val score: Int
)

/** Вопрос теста и его варианты (2–4 штуки). */
data class QuizQuestion(
    val text: String,
    val options: List<QuizOption>
)

/**
 * Тест целиком: контент живёт в коде, а не в ресурсах — это данные, а не UI-тексты.
 *
 * [interpretations] — ровно три трактовки результата: низкая, средняя и высокая.
 */
data class Quiz(
    val id: String,
    val title: String,
    val description: String,
    val questions: List<QuizQuestion>,
    val interpretations: List<String>
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
    fun interpretation(percent: Int): String =
        interpretations.getOrElse(levelOf(percent)) { interpretations.lastOrNull().orEmpty() }

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

    val quizzes: List<Quiz> = listOf(
        compatibilityQuiz(),
        loveLanguageQuiz(),
        whoIsWhoQuiz()
    )

    fun byId(id: String): Quiz? = quizzes.firstOrNull { it.id == id }

    private fun compatibilityQuiz() = Quiz(
        id = COMPATIBILITY_ID,
        title = "Совместимость",
        description = "Быт, планы, отдых и ссоры — насколько вы совпадаете в повседневности.",
        interpretations = listOf(
            "Пока вы идёте рядом, но каждый своим маршрутом. Начните с малого: полчаса в день без телефонов и разговор о планах на неделю.",
            "У вас прочная база, но быт и планы иногда расходятся. Договаривайтесь о мелочах — именно они держат пару, а не большие даты.",
            "Вы хорошо чувствуете друг друга и в быту, и в планах. Сохраняйте привычку обсуждать, а не догадываться."
        ),
        questions = listOf(
            QuizQuestion(
                text = "Как вы решаете, кто что делает по дому?",
                options = listOf(
                    QuizOption("Договариваемся и делим по-честному", 3),
                    QuizOption("Кто свободен, тот и делает", 2),
                    QuizOption("Каждый за себя, как получится", 1),
                    QuizOption("Обычно всё тянет один", 0)
                )
            ),
            QuizQuestion(
                text = "Планы на выходные вы обсуждаете…",
                options = listOf(
                    QuizOption("Заранее и вместе", 3),
                    QuizOption("В пятницу вечером, по настроению", 2),
                    QuizOption("Каждый сам по себе, потом созвонимся", 1)
                )
            ),
            QuizQuestion(
                text = "Идеальный отпуск для вас двоих — это…",
                options = listOf(
                    QuizOption("Тихо вдвоём, без толпы", 2),
                    QuizOption("Активно: города, походы, движ", 2),
                    QuizOption("Кто куда: один на море, другой на диван", 0),
                    QuizOption("Куда позовут друзья", 1)
                )
            ),
            QuizQuestion(
                text = "Если один устал и молчит, второй…",
                options = listOf(
                    QuizOption("Спрашивает, что случилось, и слушает", 3),
                    QuizOption("Даёт побыть одному, потом обнимает", 2),
                    QuizOption("Обижается на молчание", 0),
                    QuizOption("Не замечает", 1)
                )
            ),
            QuizQuestion(
                text = "Ссора у вас обычно заканчивается…",
                options = listOf(
                    QuizOption("Разговором и выводами", 3),
                    QuizOption("Обнимашками без разбора", 2),
                    QuizOption("Кто-то уходит и возвращается как ни в чём не бывало", 1),
                    QuizOption("Осадком, который всплывает снова", 0)
                )
            ),
            QuizQuestion(
                text = "Деньги в паре у вас устроены как…",
                options = listOf(
                    QuizOption("Общий бюджет и общие цели", 3),
                    QuizOption("Свои счета, общие траты по договорённости", 2),
                    QuizOption("Каждый платит за себя, тему не обсуждаем", 1)
                )
            ),
            QuizQuestion(
                text = "Планы на будущее — жильё, работа, дети…",
                options = listOf(
                    QuizOption("Обсуждаем регулярно", 3),
                    QuizOption("Говорим, когда придёт время", 2),
                    QuizOption("Избегаем сложных тем", 0),
                    QuizOption("Один уже всё решил", 1)
                )
            )
        )
    )

    private fun loveLanguageQuiz() = Quiz(
        id = LOVE_LANGUAGE_ID,
        title = "Язык любви",
        description = "Время, подарки, слова, забота и прикосновения: как вы говорите о любви.",
        interpretations = listOf(
            "Похоже, вы пока говорите на разных языках и ждёте, что другой догадается. Начните с простого вопроса: что для партнёра важнее всего?",
            "Вы чувствуете друг друга, но знаки внимания иногда теряются в делах. Добавьте одно маленькое действие в день — эффект заметен быстро.",
            "Вы умеете говорить на языке партнёра и не забываете про него в быту. Это редкий навык — берегите и подпитывайте."
        ),
        questions = listOf(
            QuizQuestion(
                text = "Вы знаете, какой язык любви у партнёра?",
                options = listOf(
                    QuizOption("Да, и стараюсь говорить на нём", 3),
                    QuizOption("Догадываюсь", 2),
                    QuizOption("Скорее знаю свой", 1),
                    QuizOption("Не думали об этом", 0)
                )
            ),
            QuizQuestion(
                text = "Когда партнёр рассказывает о проблеме, вы…",
                options = listOf(
                    QuizOption("Слушаю и спрашиваю: нужен совет или поддержка", 3),
                    QuizOption("Сразу предлагаю решение", 2),
                    QuizOption("Перевожу на свои истории", 1),
                    QuizOption("Жду, пока само отпустит", 0)
                )
            ),
            QuizQuestion(
                text = "Сколько времени в день вы проводите вдвоём без экранов?",
                options = listOf(
                    QuizOption("Больше часа", 3),
                    QuizOption("Около получаса", 2),
                    QuizOption("Иногда по пять минут", 1),
                    QuizOption("Почти никогда", 0)
                )
            ),
            QuizQuestion(
                text = "Подарки вы выбираете…",
                options = listOf(
                    QuizOption("Зная мелочи, о которых партнёр мечтал", 3),
                    QuizOption("По практичности и нужности", 2),
                    QuizOption("В последний момент", 1),
                    QuizOption("Дарим деньги", 0)
                )
            ),
            QuizQuestion(
                text = "Как вы говорите о чувствах?",
                options = listOf(
                    QuizOption("Прямо: «скучаю», «ты мне важен»", 3),
                    QuizOption("Через заботу и дела", 2),
                    QuizOption("Через подколы и шутки", 1),
                    QuizOption("Считаю, что и так понятно", 0)
                )
            ),
            QuizQuestion(
                text = "Забота в быту выглядит как…",
                options = listOf(
                    QuizOption("Замечаю мелочи и делаю без просьб", 3),
                    QuizOption("Помогаю, если попросят", 2),
                    QuizOption("Делаю своё, жду просьбы", 1),
                    QuizOption("Каждый справится сам", 0)
                )
            ),
            QuizQuestion(
                text = "Про прикосновения можно сказать…",
                options = listOf(
                    QuizOption("Тактильность у нас постоянная", 3),
                    QuizOption("Обнимаемся при встрече и на ночь", 2),
                    QuizOption("Только когда кто-то очень устал", 1),
                    QuizOption("Почти не касаемся друг друга", 0)
                )
            )
        )
    )

    private fun whoIsWhoQuiz() = Quiz(
        id = WHO_IS_WHO_ID,
        title = "Кто из вас…",
        description = "Забавный тест про привычки пары: кто мирится, кто забывает ключи и кто придумывает планы.",
        interpretations = listOf(
            "Кажется, в привычках вы живёте немного параллельно. Смешных общих ритуалов пока мало — самое время завести пару своих.",
            "Половина привычек у вас общая, половина — своя. Это нормально: главное, что вы их замечаете и можете посмеяться вместе.",
            "Вы очень «парные»: даже мелочи у вас общие. Отличный фундамент — не забывайте иногда удивлять друг друга."
        ),
        questions = listOf(
            QuizQuestion(
                text = "Кто из вас первым мирится после ссоры?",
                options = listOf(
                    QuizOption("Обычно оба", 3),
                    QuizOption("Я", 2),
                    QuizOption("Партнёр", 2),
                    QuizOption("Никто — само проходит", 0)
                )
            ),
            QuizQuestion(
                text = "Кто заводит будильник на десять минут раньше нужного?",
                options = listOf(
                    QuizOption("Оба", 3),
                    QuizOption("Только я", 2),
                    QuizOption("Только партнёр", 2),
                    QuizOption("Никто, встаём по первому", 1)
                )
            ),
            QuizQuestion(
                text = "Кто забывает, где оставил ключи?",
                options = listOf(
                    QuizOption("Оба", 3),
                    QuizOption("Только я", 2),
                    QuizOption("Только партнёр", 2),
                    QuizOption("Никто, у нас всё на месте", 1)
                )
            ),
            QuizQuestion(
                text = "Кто выбирает фильм на вечер?",
                options = listOf(
                    QuizOption("По очереди, оба участвуют", 3),
                    QuizOption("Я", 2),
                    QuizOption("Партнёр", 2),
                    QuizOption("Никто, смотрим телефоны", 0)
                )
            ),
            QuizQuestion(
                text = "Кто первым замечает, что закончился чай или сахар?",
                options = listOf(
                    QuizOption("Оба", 3),
                    QuizOption("Я", 2),
                    QuizOption("Партнёр", 2),
                    QuizOption("Никто, пока не надоест", 0)
                )
            ),
            QuizQuestion(
                text = "Кто обнимает просто так, без повода?",
                options = listOf(
                    QuizOption("Оба", 3),
                    QuizOption("Я", 2),
                    QuizOption("Партнёр", 2),
                    QuizOption("Оба забываем", 0)
                )
            ),
            QuizQuestion(
                text = "Кто придумывает планы на выходные?",
                options = listOf(
                    QuizOption("Оба предлагаем", 3),
                    QuizOption("Я", 2),
                    QuizOption("Партнёр", 2),
                    QuizOption("Планы приходят сами", 1)
                )
            )
        )
    )
}