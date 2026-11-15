package app.vdvoem.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Экземпляр виджета на главном экране. В отличие от простого списка типов,
 * каждый виджет имеет свой id и настройки — так на главной может быть
 * несколько отсчётов с разными целями.
 */
data class HomeWidget(
    val id: String,
    val type: String,
    val config: CountdownConfig = CountdownConfig()
)

/** Настройки виджета-отсчёта: что считать и в какую сторону. */
data class CountdownConfig(
    val target: String = TARGET_NEAREST,
    val eventId: String? = null,
    val direction: String = DIRECTION_UNTIL,
    val title: String? = null
) {
    val isSince: Boolean get() = direction == DIRECTION_SINCE

    companion object {
        const val TARGET_NEAREST = "nearest"
        const val TARGET_EVENT = "event"
        const val TARGET_PAIR = "pair"
        const val TARGET_BIRTHDAY_A = "birthday_a"
        const val TARGET_BIRTHDAY_B = "birthday_b"

        const val DIRECTION_UNTIL = "until"
        const val DIRECTION_SINCE = "since"
    }
}

/** JSON-кодек для списка виджетов: без лишних зависимостей, терпим к битым данным. */
object HomeWidgetCodec {

    fun encode(widgets: List<HomeWidget>): String {
        val array = JSONArray()
        widgets.forEach { widget ->
            val item = JSONObject()
            item.put("id", widget.id)
            item.put("type", widget.type)
            val config = JSONObject()
            config.put("target", widget.config.target)
            widget.config.eventId?.let { config.put("eventId", it) }
            config.put("direction", widget.config.direction)
            widget.config.title?.let { config.put("title", it) }
            item.put("config", config)
            array.put(item)
        }
        return array.toString()
    }

    fun decode(text: String): List<HomeWidget> = runCatching {
        val array = JSONArray(text)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                val type = item.optString("type").takeIf { it.isNotBlank() } ?: continue
                val config = item.optJSONObject("config")
                add(
                    HomeWidget(
                        id = id,
                        type = type,
                        config = CountdownConfig(
                            target = config?.optString("target")?.takeIf { it.isNotBlank() }
                                ?: CountdownConfig.TARGET_NEAREST,
                            eventId = config?.optString("eventId")?.takeIf { it.isNotBlank() },
                            direction = config?.optString("direction")?.takeIf { it.isNotBlank() }
                                ?: CountdownConfig.DIRECTION_UNTIL,
                            title = config?.optString("title")?.takeIf { it.isNotBlank() }
                        )
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    /** Миграция со старого формата «набор типов»: порядок — как в справочнике. */
    fun fromTypes(types: Collection<String>, known: List<String>): List<HomeWidget> =
        known.filter { it in types }.map { type -> HomeWidget(id = type, type = type) }
}