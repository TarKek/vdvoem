package app.vdvoem.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.homeDataStore: DataStore<Preferences> by preferencesDataStore("home_prefs")

/**
 * Настройки главного экрана: какие виджеты подключены и последнее отправленное сердечко.
 * Позже сюда же приедет порядок виджетов и их персональные настройки.
 */
class HomePreferences(private val context: Context) {

    private val widgetIdsKey = stringSetPreferencesKey("widget_ids")
    private val lastPokeKey = stringPreferencesKey("last_poke")

    /** Если пользователь ещё ничего не настраивал — показываем набор по умолчанию. */
    val widgetIds: Flow<List<String>> = context.homeDataStore.data
        .catch { e -> if (e is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw e }
        .map { prefs -> prefs[widgetIdsKey]?.toList() ?: DEFAULT_WIDGETS }
        .map { ids -> ids.filter { it in ALL_WIDGETS } }

    val lastPoke: Flow<String?> = context.homeDataStore.data
        .catch { e -> if (e is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw e }
        .map { prefs -> prefs[lastPokeKey] }

    suspend fun setWidgetIds(ids: List<String>) {
        context.homeDataStore.edit { prefs ->
            prefs[widgetIdsKey] = ids.filter { it in ALL_WIDGETS }.toSet()
        }
    }

    suspend fun saveLastPoke(text: String) {
        context.homeDataStore.edit { prefs -> prefs[lastPokeKey] = text }
    }

    companion object {
        const val WIDGET_COUNTDOWN = "countdown"
        const val WIDGET_EVENT = "event"
        const val WIDGET_WISHES = "wishes"
        const val WIDGET_GIFTS = "gifts"
        const val WIDGET_TESTS = "tests"
        const val WIDGET_MODULES = "modules"

        val ALL_WIDGETS = listOf(
            WIDGET_COUNTDOWN,
            WIDGET_EVENT,
            WIDGET_WISHES,
            WIDGET_GIFTS,
            WIDGET_TESTS,
            WIDGET_MODULES
        )

        val DEFAULT_WIDGETS = listOf(
            WIDGET_COUNTDOWN,
            WIDGET_EVENT,
            WIDGET_WISHES,
            WIDGET_GIFTS
        )
    }
}