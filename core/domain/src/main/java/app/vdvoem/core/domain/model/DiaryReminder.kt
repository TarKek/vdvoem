package app.vdvoem.core.domain.model

/**
 * Напоминание из дневника пары.
 *
 * [remindAt] — точный момент в миллисекундах (дата + время), а не UTC-полночь:
 * уведомление должно прийти в выбранную минуту. Личное напоминание ([isPrivate])
 * остаётся на этом телефоне и партнёру не отправляется, но попадает в ручную
 * резервную копию. Общее — синхронизируется, как записи дневника.
 */
data class DiaryReminder(
    val id: String,
    val pairId: String,
    val text: String,
    val remindAt: Long,
    val isPrivate: Boolean = false,
    val doneAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
) {
    /** Напоминание ещё актуально: не удалено и не отмечено выполненным. */
    val isActive: Boolean
        get() = deletedAt == null && doneAt == null
}