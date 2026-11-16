package app.vdvoem.core.domain.repository

import app.vdvoem.core.domain.model.CycleDay
import app.vdvoem.core.domain.model.CycleSettings
import kotlinx.coroutines.flow.Flow

/**
 * Дни трекера цикла и настройки пары.
 *
 * Данные общие: запись идёт через репозиторий (он же сигналит синку), а экран
 * получает поток и никогда не ходит в базу напрямую.
 */
interface CycleRepository {
    /** Дни без tombstone — для экрана. */
    fun observeDays(): Flow<List<CycleDay>>

    /** Включая удалённые: синку нужны tombstones. */
    fun observeDaysIncludingDeleted(): Flow<List<CycleDay>>

    fun observeSettings(): Flow<CycleSettings?>

    fun observeSettingsIncludingDeleted(): Flow<List<CycleSettings>>

    suspend fun saveDay(day: CycleDay)

    suspend fun saveSettings(settings: CycleSettings)
}