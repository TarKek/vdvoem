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

    private fun homeMoneyQuiz() = Quiz(
        id = HOME_MONEY_ID,
        title = "Быт и деньги",
        description = "Дела по дому, общий бюджет, накопления и покупки — как устроена ваша повседневность.",
        interpretations = listOf(
            "Быт и деньги у вас пока идут сами по себе, и это утомляет обоих. Начните с одного шага: раз в неделю пять минут на «кто что делает» и «сколько потратили».",
            "Основа у вас есть, но договорённости держатся на памяти, а не на правилах. Запишите распределение дел и лимит крупных трат — станет спокойнее.",
            "Вы ведёте быт и бюджет как команда: обсуждаете, планируете и не копите обид. Это самая прочная часть пары — берегите её."
        ),
        questions = listOf(
            QuizQuestion(
                text = "Как устроен ваш общий бюджет?",
                options = listOf(
                    QuizOption("Общая касса и общие цели", 3),
                    QuizOption("Свои счета, общие траты по договорённости", 2),
                    QuizOption("Кто-то один всё контролирует", 1),
                    QuizOption("Никто не знает, куда уходят деньги", 0)
                )
            ),
            QuizQuestion(
                text = "Крупная покупка — вы…",
                options = listOf(
                    QuizOption("Обсуждаем заранее и решаем вместе", 3),
                    QuizOption("Предупреждаем, но решаем сами", 2),
                    QuizOption("Каждый покупает на своё, без обсуждений", 1),
                    QuizOption("Спорим о ней уже после", 0)
                )
            ),
            QuizQuestion(
                text = "Кто помнит про счета и сроки?",
                options = listOf(
                    QuizOption("Ведём вместе или по очереди", 3),
                    QuizOption("Один, но второй в курсе", 2),
                    QuizOption("Кто вспомнил, тот и заплатил", 1),
                    QuizOption("Узнаём о долге в последний момент", 0)
                )
            ),
            QuizQuestion(
                text = "Домашние дела распределены как?",
                options = listOf(
                    QuizOption("У каждого свои зоны, и оба их знают", 3),
                    QuizOption("Делаем вместе в выходной", 2),
                    QuizOption("Кто свободен, тот и делает", 1),
                    QuizOption("Всё держится на одном человеке", 0)
                )
            ),
            QuizQuestion(
                text = "Накопления на общую цель…",
                options = listOf(
                    QuizOption("Откладываем регулярно и видим прогресс", 3),
                    QuizOption("Откладываем, когда остаётся", 2),
                    QuizOption("Планируем начать", 1),
                    QuizOption("Не копим", 0)
                )
            ),
            QuizQuestion(
                text = "Про деньги вы говорите…",
                options = listOf(
                    QuizOption("Спокойно и регулярно", 3),
                    QuizOption("Когда что-то меняется", 2),
                    QuizOption("Только когда возникла проблема", 1),
                    QuizOption("Стараемся не поднимать тему", 0)
                )
            ),
            QuizQuestion(
                text = "Продукты и мелочи для дома покупает…",
                options = listOf(
                    QuizOption("Кто свободен или вместе", 3),
                    QuizOption("В основном один, но по списку от обоих", 2),
                    QuizOption("Всегда один и без обсуждений", 1),
                    QuizOption("Покупаем хаотично, потом выбрасываем", 0)
                )
            )
        )
    )

    private fun futurePlansQuiz() = Quiz(
        id = FUTURE_PLANS_ID,
        title = "Планы на будущее",
        description = "Жильё, работа, отпуск и мечты на пять лет: смотрите ли вы в одну сторону.",
        interpretations = listOf(
            "О будущем вы почти не говорите, и каждый рисует его сам. Начните с одного вопроса: «Каким ты видишь наш следующий год?» — без споров и дедлайнов.",
            "Вы знаете главное друг о друге, но планы чаще обсуждаете, чем записываете. Одна общая цель на полгода сделает разговоры конкретнее.",
            "У вас есть общая картина будущего и привычка её сверять. Это редкое совпадение — продолжайте мечтать вслух."
        ),
        questions = listOf(
            QuizQuestion(
                text = "Каким вы видите себя через пять лет?",
                options = listOf(
                    QuizOption("Обсуждали и примерно представляем", 3),
                    QuizOption("Есть идеи, но без деталей", 2),
                    QuizOption("Каждый думает по-своему", 1),
                    QuizOption("Не заглядываем так далеко", 0)
                )
            ),
            QuizQuestion(
                text = "Большие цели — жильё, ремонт, переезд…",
                options = listOf(
                    QuizOption("Записаны и разбиты на шаги", 3),
                    QuizOption("Обсуждаем, но без плана", 2),
                    QuizOption("Вспоминаем редко", 1),
                    QuizOption("Не обсуждаем", 0)
                )
            ),
            QuizQuestion(
                text = "Отпуск через полгода…",
                options = listOf(
                    QuizOption("Уже выбрали и забронировали", 3),
                    QuizOption("Знаем направление и копим", 2),
                    QuizOption("Решим ближе к делу", 1),
                    QuizOption("Планов пока нет", 0)
                )
            ),
            QuizQuestion(
                text = "Работа и карьера партнёра для вас…",
                options = listOf(
                    QuizOption("Обсуждаем изменения и поддерживаем", 3),
                    QuizOption("Узнаём новости по факту", 2),
                    QuizOption("Считаем это личным делом", 1),
                    QuizOption("Не интересуемся", 0)
                )
            ),
            QuizQuestion(
                text = "Финансовые цели на год…",
                options = listOf(
                    QuizOption("Общие и конкретные", 3),
                    QuizOption("Есть у каждого свои", 2),
                    QuizOption("Что-то планируем", 1),
                    QuizOption("Никаких", 0)
                )
            ),
            QuizQuestion(
                text = "Мечты друг друга — путешествия, хобби, язык…",
                options = listOf(
                    QuizOption("Знаем и помогаем их осуществлять", 3),
                    QuizOption("Знаем, но редко помогаем", 2),
                    QuizOption("Знаем только часть", 1),
                    QuizOption("Почти не знаем", 0)
                )
            ),
            QuizQuestion(
                text = "Разговор о будущем обычно…",
                options = listOf(
                    QuizOption("Спокойный и конкретный", 3),
                    QuizOption("Начинается и растворяется", 2),
                    QuizOption("Перерастает в спор", 1),
                    QuizOption("Не начинается", 0)
                )
            )
        )
    )
}