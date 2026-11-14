package app.vdvoem.core.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.vdvoem.core.domain.settings.AppSettings
import app.vdvoem.core.domain.settings.AppSettingsRepository
import app.vdvoem.core.domain.settings.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

class DataStoreAppSettingsRepository(
    private val context: Context
) : AppSettingsRepository {

    override val settings: Flow<AppSettings> = context.appSettingsDataStore.data
        .catch { error -> if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw error }
        .map { prefs ->
            AppSettings(
                themeMode = prefs[KEY_THEME_MODE]
                    ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                    ?: ThemeMode.SYSTEM,
                dynamicColor = prefs[KEY_DYNAMIC_COLOR] ?: false
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[KEY_DYNAMIC_COLOR] = enabled
        }
    }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }
}
