package app.vdvoem.core.domain.model

/** Интенсивность месячных. `NONE` — день без месячных (симптомы, вес, заметка). */
enum class Flow { NONE, LIGHT, MEDIUM, HEAVY }

/**
 * Один день трекера цикла. Запись создаётся, когда есть хоть что-то: месячные,
 * симптомы, настроение, вес или заметка. Данные общие для пары — оба партнёра
 * могут править.
 *
 * `dateMillis` — начало дня (как [CalendarEvent.dateMillis]).
 * `symptoms` и `mood` — стабильные ключи, не локализованный текст:
 * подписи лежат в ресурсах экрана.
 */
data class CycleDay(
    val id: String,
    val pairId: String,
    val dateMillis: Long,
    val period: Boolean = false,
    val flow: Flow = Flow.NONE,
    /** Ключи: "cramps","headache","backache","acne","bloating","fatigue","nausea","breasts","sleep". */
    val symptoms: List<String> = emptyList(),
    /** Ключ: "happy","calm","sad","irritable","anxious","energetic". */
    val mood: String = "",
    /** Вес в килограммах, одна десятая. */
    val weight: Float? = null,
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

/** Ожидания пары по циклу. `id` совпадает с `pairId`: настройки одни на пару. */
data class CycleSettings(
    val id: String,
    val pairId: String,
    val cycleLength: Int = 28,
    val periodLength: Int = 5,
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)