package app.vdvoem.sync

import app.vdvoem.core.domain.model.CalendarEvent
import app.vdvoem.core.domain.model.Gift
import app.vdvoem.core.domain.model.GiftStatus
import app.vdvoem.core.domain.model.Pair
import app.vdvoem.core.domain.model.Wish
import app.vdvoem.core.domain.model.WishStatus
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Переносит данные пары между устройствами файлом.
 *
 * Формат — временная замена синхронизации: тот же набор сущностей, что уйдёт на сервер,
 * поэтому после появления бэкенда обмен просто переедет в сетевой слой.
 */
object PairBackup {

    const val VERSION = 1

    data class Snapshot(
        val pair: Pair?,
        val events: List<CalendarEvent>,
        val wishes: List<Wish>,
        val gifts: List<Gift>
    )

    fun export(snapshot: Snapshot): String {
        val root = JSONObject()
            .put("version", VERSION)
            .put("exportedAt", System.currentTimeMillis())

        snapshot.pair?.let { pair ->
            root.put(
                "pair",
                JSONObject()
                    .put("id", pair.id)
                    .put("nameA", pair.nameA)
                    .put("nameB", pair.nameB)
                    .put("sinceMillis", pair.sinceMillis)
                    .put("birthdayA", pair.birthdayA ?: JSONObject.NULL)
                    .put("birthdayB", pair.birthdayB ?: JSONObject.NULL)
                    .put("updatedAt", pair.updatedAt)
            )
        }

        root.put("events", JSONArray().apply {
            snapshot.events.forEach { event ->
                put(
                    JSONObject()
                        .put("id", event.id)
                        .put("pairId", event.pairId)
                        .put("title", event.title)
                        .put("dateMillis", event.dateMillis)
                        .put("annual", event.annual)
                        .put("updatedAt", event.updatedAt)
                )
            }
        })

        root.put("wishes", JSONArray().apply {
            snapshot.wishes.forEach { wish ->
                put(
                    JSONObject()
                        .put("id", wish.id)
                        .put("pairId", wish.pairId)
                        .put("title", wish.title)
                        .put("note", wish.note)
                        .put("priceEstimate", wish.priceEstimate)
                        .put("status", wish.status.name)
                        .put("updatedAt", wish.updatedAt)
                )
            }
        })

        root.put("gifts", JSONArray().apply {
            snapshot.gifts.forEach { gift ->
                put(
                    JSONObject()
                        .put("id", gift.id)
                        .put("pairId", gift.pairId)
                        .put("title", gift.title)
                        .put("note", gift.note)
                        .put("budget", gift.budget)
                        .put("status", gift.status.name)
                        .put("updatedAt", gift.updatedAt)
                )
            }
        })

        return root.toString(2)
    }

    /** Возвращает null, если файл не наш или повреждён. */
    fun parse(raw: String): Snapshot? {
        return try {
            val root = JSONObject(raw)
            if (root.optInt("version", 0) != VERSION) return null

        val pair = root.optJSONObject("pair")?.let { json ->
            Pair(
                id = json.getString("id"),
                nameA = json.getString("nameA"),
                nameB = json.getString("nameB"),
                sinceMillis = json.getLong("sinceMillis"),
                birthdayA = json.optLongOrNull("birthdayA"),
                birthdayB = json.optLongOrNull("birthdayB"),
                updatedAt = json.optLong("updatedAt", 0L)
            )
        }

        Snapshot(
            pair = pair,
            events = root.optJSONArray("events").mapObjects { json ->
                CalendarEvent(
                    id = json.getString("id"),
                    pairId = json.optString("pairId"),
                    title = json.getString("title"),
                    dateMillis = json.getLong("dateMillis"),
                    annual = json.optBoolean("annual", false),
                    updatedAt = json.optLong("updatedAt", 0L)
                )
            },
            wishes = root.optJSONArray("wishes").mapObjects { json ->
                Wish(
                    id = json.getString("id"),
                    pairId = json.optString("pairId"),
                    title = json.getString("title"),
                    note = json.optString("note"),
                    priceEstimate = json.optLongOrNull("priceEstimate"),
                    status = enumOrDefault(json.optString("status"), WishStatus.WANTED),
                    updatedAt = json.optLong("updatedAt", 0L)
                )
            },
            gifts = root.optJSONArray("gifts").mapObjects { json ->
                Gift(
                    id = json.getString("id"),
                    pairId = json.optString("pairId"),
                    title = json.getString("title"),
                    note = json.optString("note"),
                    budget = json.optLongOrNull("budget"),
                    status = enumOrDefault(json.optString("status"), GiftStatus.IDEA),
                    updatedAt = json.optLong("updatedAt", 0L)
                )
            }
        )
        } catch (e: JSONException) {
            null
        }
    }
}

private fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optJSONObject(index)?.let { add(transform(it)) }
        }
    }
}

private fun JSONObject.optLongOrNull(key: String): Long? =
    if (isNull(key)) null else optLong(key)

private inline fun <reified T : Enum<T>> enumOrDefault(name: String, default: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: default
