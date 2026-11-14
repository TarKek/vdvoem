package app.vdvoem.feature.calendar

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.calendarPreferences by preferencesDataStore(name = "calendar_prefs")

/**
 * Настройки календаря в DataStore модуля.
 *
 * Пока один переключатель — «Напоминания». Отдельный файл `calendar_prefs`,
 * чтобы настройки модуля не смешивались с чужими и не требовали правок ядра.
 */
class ReminderPreferences(private val context: Context) {

    val enabled: Flow<Boolean> = context.calendarPreferences.data
        .catch { throwable ->
            if (throwable is IOException) {
                Log.w(TAG, "Не удалось прочитать настройки напоминаний", throwable)
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences -> preferences[KEY_ENABLED] ?: false }

    suspend fun setEnabled(value: Boolean) {
        context.calendarPreferences.edit { preferences -> preferences[KEY_ENABLED] = value }
    }

    private companion object {
        const val TAG = "CalendarReminders"
        val KEY_ENABLED = booleanPreferencesKey("reminders_enabled")
    }
}