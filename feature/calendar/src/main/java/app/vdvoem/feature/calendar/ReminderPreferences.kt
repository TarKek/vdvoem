package app.vdvoem.feature.calendar

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Настройки напоминаний: включены ли, за сколько дней и в котором часу присылать. */
data class ReminderSettings(
    val enabled: Boolean = false,
    val leadDays: Int = DEFAULT_LEAD_DAYS,
    val hour: Int = DEFAULT_HOUR
) {
    companion object {
        const val DEFAULT_LEAD_DAYS = 7
        const val DEFAULT_HOUR = 9

        /** Варианты «за сколько дней» — показываются чипами в карточке напоминаний. */
        val LEAD_OPTIONS = listOf(1, 3, 7)

        /** Варианты часа отправки. */
        val HOUR_OPTIONS = listOf(9, 12, 19)
    }
}

private val Context.calendarPreferences by preferencesDataStore(name = "calendar_prefs")

/**
 * Настройки календаря в DataStore модуля.
 *
 * Отдельный файл `calendar_prefs`, чтобы настройки модуля не смешивались с чужими
 * и не требовали правок ядра.
 */
class ReminderPreferences(private val context: Context) {

    val settings: Flow<ReminderSettings> = context.calendarPreferences.data
        .catch { throwable ->
            if (throwable is IOException) {
                Log.w(TAG, "Не удалось прочитать настройки напоминаний", throwable)
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences ->
            ReminderSettings(
                enabled = preferences[KEY_ENABLED] ?: false,
                leadDays = preferences[KEY_LEAD_DAYS] ?: ReminderSettings.DEFAULT_LEAD_DAYS,
                hour = preferences[KEY_HOUR] ?: ReminderSettings.DEFAULT_HOUR
            )
        }

    /** Короткая форма для тех, кому нужен только переключатель. */
    val enabled: Flow<Boolean> = settings.map { it.enabled }

    suspend fun setEnabled(value: Boolean) {
        context.calendarPreferences.edit { preferences -> preferences[KEY_ENABLED] = value }
    }

    suspend fun setLeadDays(value: Int) {
        context.calendarPreferences.edit { preferences ->
            preferences[KEY_LEAD_DAYS] = value.coerceAtLeast(0)
        }
    }

    suspend fun setHour(value: Int) {
        context.calendarPreferences.edit { preferences ->
            preferences[KEY_HOUR] = value.coerceIn(0, 23)
        }
    }

    private companion object {
        const val TAG = "CalendarReminders"
        val KEY_ENABLED = booleanPreferencesKey("reminders_enabled")
        val KEY_LEAD_DAYS = intPreferencesKey("reminders_lead_days")
        val KEY_HOUR = intPreferencesKey("reminders_hour")
    }
}
