package app.vdvoem.feature.notes.data

/** Заметка в том виде, в котором её видит UI. */
data class Note(
    val id: String,
    val title: String,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    /** Однострочный превью-текст для карточки списка. */
    val preview: String
        get() = body.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
}