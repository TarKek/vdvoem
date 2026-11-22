package app.vdvoem.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID

private val Context.homeDataStore: DataStore<Preferences> by preferencesDataStore("home_prefs")

/**
 * Настройки главного экрана: какие виджеты подключены, их порядок и настройки,
 * плюс последнее отправленное сердечко.
 */
class HomePreferences(private val context: Context) {

    private val widgetsKey = stringPreferencesKey("widgets_json")
    private val legacyWidgetIdsKey = stringSetPreferencesKey("widget_ids")
    private val lastPokeKey = stringPreferencesKey("last_poke")
    private val sentPokeIdsKey = stringSetPreferencesKey("sent_poke_ids")
    private val navRoutesKey = stringPreferencesKey("nav_routes")

    /** Если пользователь ещё ничего не настраивал — показываем набор по умолчанию. */
    val widgets: Flow<List<HomeWidget>> = context.homeDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs -> readWidgets(prefs) }

    val lastPoke: Flow<String?> = context.homeDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs -> prefs[lastPokeKey] }

    /** Идентификаторы сердечек, отправленных с этого телефона — чтобы отличать их в истории. */
    val sentPokeIds: Flow<Set<String>> = context.homeDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs -> prefs[sentPokeIdsKey].orEmpty() }

    suspend fun addSentPokeId(id: String) {
        context.homeDataStore.edit { prefs ->
            val current = prefs[sentPokeIdsKey].orEmpty()
            prefs[sentPokeIdsKey] = (current + id).toList().takeLast(MAX_SENT_POKE_IDS).toSet()
        }
    }

    suspend fun setWidgets(widgets: List<HomeWidget>) {
        context.homeDataStore.edit { prefs ->
            prefs[widgetsKey] = HomeWidgetCodec.encode(widgets.filter { it.type in ALL_WIDGETS })
        }
    }

    suspend fun addWidget(type: String) {
        if (type !in ALL_WIDGETS) return
        context.homeDataStore.edit { prefs ->
            val current = readWidgets(prefs)
            prefs[widgetsKey] = HomeWidgetCodec.encode(
                current + HomeWidget(id = newId(type), type = type)
            )
        }
    }

    suspend fun updateWidget(widget: HomeWidget) {
        context.homeDataStore.edit { prefs ->
            val current = readWidgets(prefs)
            prefs[widgetsKey] = HomeWidgetCodec.encode(
                current.map { if (it.id == widget.id) widget else it }
            )
        }
    }

    suspend fun removeWidget(id: String) {
        context.homeDataStore.edit { prefs ->
            val current = readWidgets(prefs)
            prefs[widgetsKey] = HomeWidgetCodec.encode(current.filterNot { it.id == id })
        }
    }

    /** Сдвиг виджета на одну позицию: -1 вверх, +1 вниз. */
    suspend fun moveWidget(id: String, delta: Int) {
        context.homeDataStore.edit { prefs ->
            val current = readWidgets(prefs).toMutableList()
            val from = current.indexOfFirst { it.id == id }
            if (from < 0) return@edit
            val to = (from + delta).coerceIn(0, current.lastIndex)
            if (to == from) return@edit
            val item = current.removeAt(from)
            current.add(to, item)
            prefs[widgetsKey] = HomeWidgetCodec.encode(current)
        }
    }

    suspend fun saveLastPoke(text: String) {
        context.homeDataStore.edit { prefs -> prefs[lastPokeKey] = text }
    }

    /**
     * Маршруты двух сменных вкладок нижней панели.
     *
     * «Главная» и «Ещё» закреплены и здесь не хранятся. Если сохранённых маршрутов
     * не хватает или они пустые — добираем значениями по умолчанию.
     */
    val navRoutes: Flow<List<String>> = context.homeDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs -> readNavRoutes(prefs) }

    suspend fun setNavRoutes(routes: List<String>) {
        context.homeDataStore.edit { prefs ->
            prefs[navRoutesKey] = routes
                .filter { it.isNotBlank() }
                .take(NAV_SLOTS)
                .joinToString(",")
        }
    }

    private fun readNavRoutes(prefs: Preferences): List<String> {
        val raw = prefs[navRoutesKey] ?: return DEFAULT_NAV_ROUTES
        return raw.split(',')
            .filter { it.isNotBlank() }
            .take(NAV_SLOTS)
    }

    private fun readWidgets(prefs: Preferences): List<HomeWidget> {
        val stored = prefs[widgetsKey]?.let { HomeWidgetCodec.decode(it) }
        val widgets = when {
            stored != null -> stored
            else -> prefs[legacyWidgetIdsKey]
                ?.let { HomeWidgetCodec.fromTypes(it, ALL_WIDGETS) }
                ?: DEFAULT_WIDGETS
        }
        return widgets.filter { it.type in ALL_WIDGETS }
    }

    private fun newId(type: String): String =
        "$type-${UUID.randomUUID().toString().take(8)}"

    companion object {
        /** Сколько идентификаторов отправленных сердечек помнить локально. */
        private const val MAX_SENT_POKE_IDS = 200

        /** Сколько сменных вкладок в нижней панели (плюс закреплённые «Главная» и «Ещё»). */
        const val NAV_SLOTS = 2

        val DEFAULT_NAV_ROUTES = listOf("presents", "timetable")

        const val WIDGET_COUNTDOWN = "countdown"
        const val WIDGET_EVENT = "event"
        const val WIDGET_WISHES = "wishes"
        const val WIDGET_GIFTS = "gifts"
        const val WIDGET_TESTS = "tests"
        const val WIDGET_MODULES = "modules"
        const val WIDGET_CYCLE = "cycle"
        const val WIDGET_TOGETHER = "together"
        const val WIDGET_DIARY = "diary"

        val ALL_WIDGETS = listOf(
            WIDGET_COUNTDOWN,
            WIDGET_EVENT,
            WIDGET_WISHES,
            WIDGET_GIFTS,
            WIDGET_TESTS,
            WIDGET_MODULES,
            WIDGET_CYCLE,
            WIDGET_TOGETHER,
            WIDGET_DIARY
        )

        val DEFAULT_WIDGETS = listOf(
            HomeWidget(id = WIDGET_COUNTDOWN, type = WIDGET_COUNTDOWN),
            HomeWidget(id = WIDGET_EVENT, type = WIDGET_EVENT),
            HomeWidget(id = WIDGET_WISHES, type = WIDGET_WISHES),
            HomeWidget(id = WIDGET_GIFTS, type = WIDGET_GIFTS)
        )
    }
}