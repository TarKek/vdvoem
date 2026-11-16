package app.vdvoem.feature.gifts

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.giftBudgetDataStore by preferencesDataStore(name = "gift_budget")

/**
 * Годовой бюджет на подарки в DataStore модуля.
 *
 * Отдельный файл `gift_budget`, чтобы настройка модуля не смешивалась с чужими
 * и не требовала правок ядра.
 */
class GiftBudgetPreferences(private val context: Context) {

    /** `null` — бюджет не задан. */
    val budget: Flow<Long?> = context.giftBudgetDataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                Log.w(TAG, "Не удалось прочитать бюджет подарков", throwable)
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences -> preferences[KEY_YEARLY_BUDGET]?.takeIf { it > 0L } }

    suspend fun setBudget(value: Long?) {
        context.giftBudgetDataStore.edit { preferences ->
            if (value == null || value <= 0L) {
                preferences.remove(KEY_YEARLY_BUDGET)
            } else {
                preferences[KEY_YEARLY_BUDGET] = value
            }
        }
    }

    private companion object {
        const val TAG = "GiftBudget"
        val KEY_YEARLY_BUDGET = longPreferencesKey("yearly_budget")
    }
}