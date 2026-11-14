package app.vdvoem.feature.quizzes

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Последнее прохождение теста: процент и момент прохождения. */
data class QuizResult(
    val quizId: String,
    val percent: Int,
    val passedAt: Long
)

private val Context.quizPreferences by preferencesDataStore(name = QuizResultStore.STORE_NAME)

/**
 * Результаты тестов в DataStore модуля.
 *
 * Хранится только последнее прохождение каждого теста — этого достаточно для списка
 * и для оффлайн-работы. Ключ — идентификатор теста, значение — «процент|время».
 */
class QuizResultStore(private val context: Context) {

    /** Результаты по идентификатору теста. */
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
                val raw = value as? String ?: return@mapNotNull null
                val parts = raw.split(SEPARATOR)
                val percent = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
                val passedAt = parts.getOrNull(1)?.toLongOrNull() ?: 0L
                val quizId = name.removePrefix(KEY_PREFIX)
                quizId to QuizResult(
                    quizId = quizId,
                    percent = percent.coerceIn(0, 100),
                    passedAt = passedAt
                )
            }.toMap()
        }

    suspend fun save(quizId: String, percent: Int, passedAt: Long) {
        context.quizPreferences.edit { preferences ->
            preferences[stringPreferencesKey(KEY_PREFIX + quizId)] =
                "${percent.coerceIn(0, 100)}$SEPARATOR$passedAt"
        }
    }

    companion object {
        const val STORE_NAME = "quiz_results"

        private const val TAG = "Quizzes"
        private const val KEY_PREFIX = "result_"
        private const val SEPARATOR = "|"
    }
}