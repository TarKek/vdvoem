package app.vdvoem.sync

import app.vdvoem.core.domain.model.CalendarEvent
import app.vdvoem.core.domain.model.CycleDay
import app.vdvoem.core.domain.model.CycleSettings
import app.vdvoem.core.domain.model.Flow
import app.vdvoem.core.domain.model.Gift
import app.vdvoem.core.domain.model.GiftStatus
import app.vdvoem.core.domain.model.Lesson
import app.vdvoem.core.domain.model.Pair
import app.vdvoem.core.domain.model.Poke
import app.vdvoem.core.domain.model.QuizScore
import app.vdvoem.core.domain.model.SharedTask
import app.vdvoem.core.domain.model.Wish
import app.vdvoem.core.domain.model.WishStatus
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID

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
        val gifts: List<Gift>,
        val pokes: List<Poke> = emptyList(),
        val quizScores: List<QuizScore> = emptyList(),
        val lessons: List<Lesson> = emptyList(),
        val tasks: List<SharedTask> = emptyList(),
        val cycleDays: List<CycleDay> = emptyList(),
        val cycleSettings: CycleSettings? = null
    ) {
        /** Сколько записей реально переносится — для сообщения «Загружено записей». */
        val total: Int
            get() = events.size + wishes.size + gifts.size + pokes.size + quizScores.size +
                lessons.size + tasks.size + cycleDays.size + (if (cycleSettings != null) 1 else 0)
    }

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
                        .put("deletedAt", event.deletedAt ?: JSONObject.NULL)
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
                        .put("deletedAt", wish.deletedAt ?: JSONObject.NULL)
                        .put("priority", wish.priority)
                        .put("url", wish.url)
                        .put("surprise", wish.surprise)
                        .put("ownerDeviceId", wish.ownerDeviceId)
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
                        .put("deletedAt", gift.deletedAt ?: JSONObject.NULL)
                        .put("wishId", gift.wishId ?: JSONObject.NULL)
                        .put("surprise", gift.surprise)
                        .put("ownerDeviceId", gift.ownerDeviceId)
                        .put("boughtAt", gift.boughtAt ?: JSONObject.NULL)
                        .put("givenAt", gift.givenAt ?: JSONObject.NULL)
                )
            }
        })

        root.put("pokes", JSONArray().apply {
            snapshot.pokes.forEach { poke ->
                put(
                    JSONObject()
                        .put("id", poke.id)
                        .put("pairId", poke.pairId)
                        .put("text", poke.text)
                        .put("createdAt", poke.createdAt)
                        .put("updatedAt", poke.updatedAt)
                        .put("deletedAt", poke.deletedAt ?: JSONObject.NULL)
                )
            }
        })

        root.put("quizScores", JSONArray().apply {
            snapshot.quizScores.forEach { score ->
                put(
                    JSONObject()
                        .put("id", score.id)
                        .put("pairId", score.pairId)
                        .put("quizId", score.quizId)
                        .put("percent", score.percent)
                        .put("passedAt", score.passedAt)
                        .put("updatedAt", score.updatedAt)
                        .put("deletedAt", score.deletedAt ?: JSONObject.NULL)
                )
            }
        })

        root.put("lessons", JSONArray().apply {
            snapshot.lessons.forEach { lesson ->
                put(
                    JSONObject()
                        .put("id", lesson.id)
                        .put("pairId", lesson.pairId)
                        .put("subject", lesson.subject)
                        .put("weekday", lesson.weekday)
                        .put("startMinute", lesson.startMinute)
                        .put("endMinute", lesson.endMinute)
                        .put("room", lesson.room)
                        .put("teacher", lesson.teacher)
                        .put("textbookBy", lesson.textbookBy)
                        .put("updatedAt", lesson.updatedAt)
                        .put("deletedAt", lesson.deletedAt ?: JSONObject.NULL)
                )
            }
        })

        root.put("tasks", JSONArray().apply {
            snapshot.tasks.forEach { task ->
                put(
                    JSONObject()
                        .put("id", task.id)
                        .put("pairId", task.pairId)
                        .put("title", task.title)
                        .put("note", task.note)
                        .put("dueAt", task.dueAt ?: JSONObject.NULL)
                        .put("isCompleted", task.isCompleted)
                        .put("createdBy", task.createdBy)
                        .put("createdAt", task.createdAt)
                        .put("updatedAt", task.updatedAt)
                        .put("deletedAt", task.deletedAt ?: JSONObject.NULL)
                        .put("version", task.version)
                )
            }
        })

        root.put("cycleDays", JSONArray().apply {
            snapshot.cycleDays.forEach { day ->
                put(
                    JSONObject()
                        .put("id", day.id)
                        .put("pairId", day.pairId)
                        .put("dateMillis", day.dateMillis)
                        .put("period", day.period)
                        .put("flow", day.flow.name)
                        .put("symptoms", JSONArray(day.symptoms))
                        .put("mood", day.mood)
                        .put("weight", day.weight?.takeIf { it.isFinite() && it in 20f..300f } ?: JSONObject.NULL)
                        .put("note", day.note)
                        .put("updatedAt", day.updatedAt)
                        .put("deletedAt", day.deletedAt ?: JSONObject.NULL)
                )
            }
        })

        snapshot.cycleSettings?.let { settings ->
            root.put(
                "cycleSettings",
                JSONObject()
                    .put("id", settings.id)
                    .put("pairId", settings.pairId)
                    .put("cycleLength", settings.cycleLength)
                    .put("periodLength", settings.periodLength)
                    .put("updatedAt", settings.updatedAt)
                    .put("deletedAt", settings.deletedAt ?: JSONObject.NULL)
            )
        }

        return root.toString(2)
    }

    fun hasValidIds(snapshot: Snapshot): Boolean = runCatching {
        val ids = buildList {
            snapshot.pair?.let { add(it.id) }
            snapshot.events.forEach { add(it.id) }
            snapshot.wishes.forEach { add(it.id) }
            snapshot.gifts.forEach { add(it.id) }
            snapshot.pokes.forEach { add(it.id) }
            snapshot.quizScores.forEach { add(it.id) }
            snapshot.lessons.forEach { add(it.id) }
            snapshot.tasks.forEach { add(it.id) }
            snapshot.cycleDays.forEach { add(it.id) }
            snapshot.cycleSettings?.let { add(it.id) }
        }
        ids.forEach { UUID.fromString(it) }
    }.isSuccess

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

        val snapshot = Snapshot(
            pair = pair,
            events = root.optJSONArray("events").mapObjects { json ->
                CalendarEvent(
                    id = json.getString("id"),
                    pairId = json.optString("pairId"),
                    title = json.getString("title"),
                    dateMillis = json.getLong("dateMillis"),
                    annual = json.optBoolean("annual", false),
                    updatedAt = json.optLong("updatedAt", 0L),
                    deletedAt = json.optLongOrNull("deletedAt")
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
                    updatedAt = json.optLong("updatedAt", 0L),
                    priority = json.optInt("priority", Wish.PRIORITY_NORMAL),
                    url = json.optString("url"),
                    surprise = json.optBoolean("surprise", false),
                    ownerDeviceId = json.optString("ownerDeviceId"),
                    deletedAt = json.optLongOrNull("deletedAt")
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
                    updatedAt = json.optLong("updatedAt", 0L),
                    wishId = if (json.isNull("wishId")) null else json.optString("wishId").ifBlank { null },
                    deletedAt = json.optLongOrNull("deletedAt"),
                    surprise = json.optBoolean("surprise", false),
                    ownerDeviceId = json.optString("ownerDeviceId"),
                    boughtAt = json.optLongOrNull("boughtAt"),
                    givenAt = json.optLongOrNull("givenAt")
                )
            },
            pokes = root.optJSONArray("pokes").mapObjects { json ->
                Poke(
                    id = json.getString("id"),
                    pairId = json.optString("pairId"),
                    text = json.optString("text"),
                    createdAt = json.optLong("createdAt", 0L),
                    updatedAt = json.optLong("updatedAt", 0L),
                    deletedAt = json.optLongOrNull("deletedAt")
                )
            },
            quizScores = root.optJSONArray("quizScores").mapObjects { json ->
                QuizScore(
                    id = json.getString("id"),
                    pairId = json.optString("pairId"),
                    quizId = json.optString("quizId"),
                    percent = json.optInt("percent", 0),
                    passedAt = json.optLong("passedAt", 0L),
                    updatedAt = json.optLong("updatedAt", 0L),
                    deletedAt = json.optLongOrNull("deletedAt")
                )
            },
            lessons = root.optJSONArray("lessons").mapObjects { json ->
                Lesson(
                    id = json.getString("id"),
                    pairId = json.optString("pairId"),
                    subject = json.optString("subject"),
                    weekday = json.optInt("weekday", 1),
                    startMinute = json.optInt("startMinute", 0),
                    endMinute = json.optInt("endMinute", 0),
                    room = json.optString("room"),
                    teacher = json.optString("teacher"),
                    textbookBy = json.optString("textbookBy"),
                    updatedAt = json.optLong("updatedAt", 0L),
                    deletedAt = json.optLongOrNull("deletedAt")
                )
            },
            tasks = root.optJSONArray("tasks").mapObjects { json ->
                SharedTask(
                    id = json.getString("id"),
                    pairId = json.optString("pairId"),
                    title = json.optString("title"),
                    note = json.optString("note"),
                    dueAt = json.optLongOrNull("dueAt"),
                    isCompleted = json.optBoolean("isCompleted", false),
                    createdBy = json.optString("createdBy"),
                    createdAt = json.optLong("createdAt", 0L),
                    updatedAt = json.optLong("updatedAt", 0L),
                    deletedAt = json.optLongOrNull("deletedAt"),
                    version = json.optLong("version", 1L).coerceAtLeast(1L)
                )
            },
            cycleDays = root.optJSONArray("cycleDays").mapObjects { json ->
                CycleDay(
                    id = json.getString("id"),
                    pairId = json.optString("pairId"),
                    dateMillis = json.optLong("dateMillis", 0L),
                    period = json.optBoolean("period", false),
                    flow = enumOrDefault(json.optString("flow"), Flow.NONE),
                    symptoms = json.optJSONArray("symptoms").toStringList(),
                    mood = json.optString("mood"),
                    weight = if (json.isNull("weight")) null else
                        json.optDouble("weight", 0.0).toFloat().takeIf { it.isFinite() && it in 20f..300f },
                    note = json.optString("note"),
                    updatedAt = json.optLong("updatedAt", 0L),
                    deletedAt = json.optLongOrNull("deletedAt")
                )
            },
            cycleSettings = root.optJSONObject("cycleSettings")?.let { json ->
                CycleSettings(
                    id = json.getString("id"),
                    pairId = json.optString("pairId"),
                    cycleLength = json.optInt("cycleLength", 28),
                    periodLength = json.optInt("periodLength", 5),
                    updatedAt = json.optLong("updatedAt", 0L),
                    deletedAt = json.optLongOrNull("deletedAt")
                )
            }
        )
        snapshot
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

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optString(index).takeIf { it.isNotBlank() }?.let { add(it) }
        }
    }
}

private inline fun <reified T : Enum<T>> enumOrDefault(name: String, default: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: default
