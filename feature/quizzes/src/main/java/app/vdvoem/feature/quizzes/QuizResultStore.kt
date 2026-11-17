package app.vdvoem.feature.quizzes

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Одно прохождение теста: процент и момент прохождения. */
data class QuizAttempt(
    val percent: Int,
    val passedAt: Long
)

/**
 * История прохождений теста: до [QuizResultStore.MAX_ATTEMPTS] записей, от старых к новым.
 *
 * [percent] и [passedAt] — последнее прохождение, [bestPercent] — лучший результат за всё время.
 */
data class QuizResult(
    val quizId: String,
    val attempts: List<QuizAttempt> = emptyList(),
    val storedBestPercent: Int? = null
) {
    val hasAttempts: Boolean
        get() = attempts.isNotEmpty()

    val percent: Int
        get() = attempts.lastOrNull()?.percent ?: 0

    val passedAt: Long
        get() = attempts.lastOrNull()?.passedAt ?: 0L

    val bestPercent: Int
        get() = maxOf(storedBestPercent ?: 0, attempts.maxOfOrNull { it.percent } ?: 0)

    val bestPassedAt: Long
        get() = attempts.firstOrNull { it.percent == bestPercent }?.passedAt ?: 0L
}

private val Context.quizPreferences by preferencesDataStore(name = QuizResultStore.STORE_NAME)

/**
 * Результаты тестов в DataStore модуля.
 *
 * Хранится до [MAX_ATTEMPTS] последних прохождений каждого теста — этого достаточно
 * для списка, лучшего результата и оффлайн-работы. Ключ — идентификатор теста,
 * значение — список пар «процент|время», разделённых [ENTRY_SEPARATOR].
 */
class QuizResultStore(private val context: Context) {

    /** Истории прохождений по идентификатору теста. */
    val results: Flow<Map<String, QuizResult>> = context.quizPreferences.data
        .catch { throwable ->
            if (throwable is IOException) {
                Log.w(TAG, "Не удалось прочитать результаты тестов", throwable)
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences ->
            preferences.asMap().mapNotNull { (key, value) ->
                val name = key.name
                if (!name.startsWith(KEY_PREFIX)) return@mapNotNull null
                val attempts = parseAttempts(value as? String) ?: return@mapNotNull null
                if (attempts.isEmpty()) return@mapNotNull null
                val quizId = name.removePrefix(KEY_PREFIX)
                quizId to QuizResult(
                    quizId = quizId,
                    attempts = attempts,
                    storedBestPercent = preferences[intPreferencesKey(BEST_PREFIX + quizId)]
                )
            }.toMap()
        }

    /** Идентификаторы прохождений, сделанных на этом телефоне — чтобы отличать свои от партнёрских. */
    val ownScoreIds: Flow<Set<String>> = context.quizPreferences.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences -> preferences[ownScoreIdsKey].orEmpty() }

    suspend fun addOwnScoreId(id: String) {
        context.quizPreferences.edit { preferences ->
            val current = preferences[ownScoreIdsKey].orEmpty()
            preferences[ownScoreIdsKey] =
                (current + id).toList().takeLast(MAX_OWN_IDS).toSet()
        }
    }

    /** Добавляет прохождение в историю, оставляя последние [MAX_ATTEMPTS] записей. */
    suspend fun save(quizId: String, percent: Int, passedAt: Long) {
        val key = stringPreferencesKey(KEY_PREFIX + quizId)
        val bestKey = intPreferencesKey(BEST_PREFIX + quizId)
        context.quizPreferences.edit { preferences ->
            val safePercent = percent.coerceIn(0, 100)
            val attempts = (parseAttempts(preferences[key]).orEmpty() +
                QuizAttempt(percent = safePercent, passedAt = passedAt))
                .takeLast(MAX_ATTEMPTS)
            preferences[key] = attempts.joinToString(ENTRY_SEPARATOR) { attempt ->
                "${attempt.percent}$SEPARATOR${attempt.passedAt}"
            }
            preferences[bestKey] = maxOf(preferences[bestKey] ?: 0, safePercent)
        }
    }

    /** Читает «82|1699999999;70|1698888888»; старый формат без разделителя тоже подходит. */
    private fun parseAttempts(raw: String?): List<QuizAttempt>? {
        if (raw.isNullOrBlank()) return null
        return raw.split(ENTRY_SEPARATOR)
            .mapNotNull { entry ->
                val parts = entry.split(SEPARATOR)
                val percent = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: return@mapNotNull null
                val passedAt = parts.getOrNull(1)?.trim()?.toLongOrNull() ?: 0L
                QuizAttempt(percent = percent.coerceIn(0, 100), passedAt = passedAt)
            }
            .takeLast(MAX_ATTEMPTS)
    }

    companion object {
        const val STORE_NAME = "quiz_results"

        /** Сколько прохождений хранить на тест. */
        const val MAX_ATTEMPTS = 5

        private const val TAG = "Quizzes"
        private val ownScoreIdsKey = stringSetPreferencesKey("own_score_ids")
        private const val MAX_OWN_IDS = 200
        private const val KEY_PREFIX = "result_"
        private const val BEST_PREFIX = "best_"
        private const val SEPARATOR = "|"
        private const val ENTRY_SEPARATOR = ";"
    }
}