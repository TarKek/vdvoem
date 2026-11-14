package app.vdvoem.core.domain.model

/** Событие пары: годовщина, встреча, поездка. `annual` — повторяется каждый год. */
data class CalendarEvent(
    val id: String,
    val pairId: String,
    val title: String,
    val dateMillis: Long,
    val annual: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
