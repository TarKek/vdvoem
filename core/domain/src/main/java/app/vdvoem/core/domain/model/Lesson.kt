package app.vdvoem.core.domain.model

/**
 * Урок в расписании пары.
 *
 * [weekday] — 1 = понедельник … 7 = воскресенье (как в ISO).
 * [startMinute] / [endMinute] — минуты от полуночи, чтобы не тащить часовые пояса.
 */
data class Lesson(
    val id: String,
    val pairId: String = "",
    val subject: String,
    val weekday: Int,
    val startMinute: Int,
    val endMinute: Int,
    val room: String = "",
    val teacher: String = "",
    /** Кто берёт учебник на этот предмет: свободный текст («Я», «Варя», «оба»). */
    val textbookBy: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
) {
    /** «9:00–9:45» одним куском для карточки. */
    val timeRange: String
        get() = "${formatMinute(startMinute)}–${formatMinute(endMinute)}"

    companion object {
        /** «09:05» → 545. */
        fun parseMinute(text: String): Int? {
            val parts = text.trim().split(':', '.', ' ')
            if (parts.size != 2) return null
            val hour = parts[0].toIntOrNull() ?: return null
            val minute = parts[1].toIntOrNull() ?: return null
            if (hour == 24 && minute == 0) return 24 * 60
            if (hour !in 0..23 || minute !in 0..59) return null
            return hour * 60 + minute
        }

        /** 545 → «9:05» (без ведущего нуля у часа — так читается компактнее). */
        fun formatMinute(value: Int): String {
            val safe = value.coerceIn(0, 24 * 60)
            return "${safe / 60}:${(safe % 60).toString().padStart(2, '0')}"
        }
    }
}