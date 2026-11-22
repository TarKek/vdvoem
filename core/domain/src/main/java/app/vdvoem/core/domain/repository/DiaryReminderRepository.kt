package app.vdvoem.core.domain.repository

import app.vdvoem.core.domain.model.DiaryReminder
import kotlinx.coroutines.flow.Flow

/**
 * Напоминания из дневника пары.
 *
 * Личные напоминания живут только на устройстве: синхронизация их не отправляет
 * и не принимает, но резервная копия пары их сохраняет. Удаление — мягкое
 * (tombstone), чтобы общее напоминание не «оживало» от копии партнёра.
 */
interface DiaryReminderRepository {
    /** Активные напоминания текущей пары по времени, от ближайшего. */
    fun observeReminders(): Flow<List<DiaryReminder>>

    /** Включая выполненные и удалённые: синку нужны tombstone, экрану — нет. */
    fun observeRemindersIncludingDeleted(): Flow<List<DiaryReminder>>

    suspend fun upsert(reminder: DiaryReminder)

    /** Отметить выполненным или снять отметку. */
    suspend fun setDone(id: String, done: Boolean)

    suspend fun delete(id: String)
}