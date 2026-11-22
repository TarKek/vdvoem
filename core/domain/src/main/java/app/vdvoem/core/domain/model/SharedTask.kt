package app.vdvoem.core.domain.model

/**
 * Общее дело пары.
 *
 * Дата срока хранится как UTC-полночь (см. `Dates`), без времени и часового пояса.
 * [createdBy] — стабильный идентификатор установки автора. [version] растёт при каждой
 * локальной правке, а для конфликтов синхронизации используется общий LWW по [updatedAt].
 */
data class SharedTask(
    val id: String,
    val pairId: String = "",
    val title: String,
    val note: String = "",
    val dueAt: Long? = null,
    val isCompleted: Boolean = false,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val deletedAt: Long? = null,
    val version: Long = 1L
) {
    /** Возвращает изменённую копию; неизменённый редактор не создаёт лишнюю синхронизацию. */
    fun edit(title: String, note: String, dueAt: Long?, now: Long): SharedTask {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "Task title must not be blank" }
        val cleanNote = note.trim()
        if (cleanTitle == this.title && cleanNote == this.note && dueAt == this.dueAt) return this
        return copy(
            title = cleanTitle,
            note = cleanNote,
            dueAt = dueAt,
            updatedAt = nextTimestamp(updatedAt, now),
            version = version + 1
        )
    }

    /** Повторное нажатие с тем же состоянием не меняет запись и её версию. */
    fun withCompletion(completed: Boolean, now: Long): SharedTask {
        if (isCompleted == completed) return this
        return copy(
            isCompleted = completed,
            updatedAt = nextTimestamp(updatedAt, now),
            version = version + 1
        )
    }

    companion object {
        fun create(
            id: String,
            pairId: String,
            createdBy: String,
            title: String,
            note: String,
            dueAt: Long?,
            now: Long
        ): SharedTask {
            val cleanTitle = title.trim()
            require(cleanTitle.isNotEmpty()) { "Task title must not be blank" }
            return SharedTask(
                id = id,
                pairId = pairId,
                title = cleanTitle,
                note = note.trim(),
                dueAt = dueAt,
                createdBy = createdBy,
                createdAt = now,
                updatedAt = now
            )
        }

        /** Две быстрые правки в одну миллисекунду не должны проиграть LWW самим себе. */
        private fun nextTimestamp(previous: Long, now: Long): Long = maxOf(now, previous + 1)
    }
}
