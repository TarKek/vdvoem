package app.vdvoem.core.data.settings

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.vdvoem.core.domain.repository.ModuleSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.modulePreferences by preferencesDataStore(name = "module_settings")

/**
 * Локальное состояние модулей в DataStore.
 *
 * Пока пользователь ни разу не менял настройки, возвращаются значения по умолчанию из реестра —
 * так новые модули, помеченные defaultEnabled, появляются у всех без миграций.
 */
class DataStoreModuleSettingsRepository(
    private val context: Context,
    private val defaultEnabledIds: Set<String>
) : ModuleSettingsRepository {

    override val enabledIds: Flow<Set<String>> = context.modulePreferences.data
        .catch { throwable ->
            if (throwable is IOException) {
                Log.w(TAG, "Не удалось прочитать настройки модулей, берём значения по умолчанию", throwable)
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences ->
            if (preferences[KEY_INITIALIZED] == true) {
                migrated(preferences)
            } else {
                defaultEnabledIds
            }
        }

    override suspend fun setEnabled(moduleId: String, enabled: Boolean) {
        context.modulePreferences.edit { preferences ->
            val current = if (preferences[KEY_INITIALIZED] == true) {
                migrated(preferences)
            } else {
                defaultEnabledIds
            }
            preferences[KEY_ENABLED] = if (enabled) current + moduleId else current - moduleId
            preferences[KEY_INITIALIZED] = true
            preferences[KEY_SCHEMA] = SCHEMA
        }
    }

    /** После смены состава модулей новые defaultEnabled-модули включаются один раз. */
    private fun migrated(preferences: androidx.datastore.preferences.core.Preferences): Set<String> {
        val stored = preferences[KEY_ENABLED] ?: emptySet()
        val schema = preferences[KEY_SCHEMA] ?: 0
        return if (schema < SCHEMA) stored + defaultEnabledIds else stored
    }

    private companion object {
        const val TAG = "ModuleSettings"

        /** Увеличивать при изменении набора модулей, чтобы новые включались у всех. */
        const val SCHEMA = 2

        val KEY_ENABLED = stringSetPreferencesKey("enabled_module_ids")
        val KEY_INITIALIZED = booleanPreferencesKey("initialized")
        val KEY_SCHEMA = intPreferencesKey("modules_schema")
    }
}
