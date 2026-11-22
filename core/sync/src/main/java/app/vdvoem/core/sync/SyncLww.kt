package app.vdvoem.core.sync

/**
 * Правила разрешения конфликтов синхронизации (last-write-wins) и водяной знак отправки.
 *
 * Вынесено из [SyncEngine] отдельным файлом, чтобы правила можно было покрыть JVM-тестами
 * без Android-зависимостей (Room, DataStore, WorkManager).
 */

/**
 * Нужно ли применить серверную запись к локальной.
 *
 * - локальной записи нет — применяем всегда;
 * - серверная не новее локальной — не трогаем;
 * - локальная удалена (tombstone) — удаление окончательное, «воскрешать» запись нельзя.
 */
internal fun shouldApplyRemote(
    localUpdatedAt: Long?,
    localDeletedAt: Long?,
    remoteUpdatedAt: Long
): Boolean {
    if (localUpdatedAt == null) return true
    return remoteUpdatedAt > localUpdatedAt
}

/** Самый свежий `updatedAt` в отправляемом наборе — водяной знак отправки. */
internal fun ChangesDto.maxUpdatedAt(): Long = (
    listOfNotNull(pair?.updatedAt) +
        events.map { it.updatedAt } +
        wishes.map { it.updatedAt } +
        gifts.map { it.updatedAt } +
        pokes.map { it.updatedAt } +
        quizScores.map { it.updatedAt } +
        lessons.map { it.updatedAt } +
        tasks.map { it.updatedAt } +
        cycleDays.map { it.updatedAt } +
        cycleSettings.map { it.updatedAt } +
        diaryEntries.map { it.updatedAt } +
        diaryReminders.map { it.updatedAt }
    ).maxOrNull() ?: 0L