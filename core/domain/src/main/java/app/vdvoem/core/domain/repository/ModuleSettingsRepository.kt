package app.vdvoem.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Состояние модулей: какие включены.
 *
 * Хранится локально. Позже сюда же придёт синхронизация, но интерфейс останется тем же,
 * поэтому фичи не знают, откуда берутся настройки.
 */
interface ModuleSettingsRepository {

    /** Включённые модули. Пока пользователь не менял настройки — значения по умолчанию. */
    val enabledIds: Flow<Set<String>>

    suspend fun setEnabled(moduleId: String, enabled: Boolean)
}
