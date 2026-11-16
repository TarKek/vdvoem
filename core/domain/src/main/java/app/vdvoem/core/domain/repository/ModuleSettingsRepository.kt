package app.vdvoem.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Состояние модулей: какие включены, в каком порядке идут и их локальные настройки.
 *
 * Хранится локально. Позже сюда же придёт синхронизация, но интерфейс останется тем же,
 * поэтому фичи не знают, откуда берутся настройки.
 */
interface ModuleSettingsRepository {

    /** Включённые модули. Пока пользователь не менял настройки — значения по умолчанию. */
    val enabledIds: Flow<Set<String>>

    suspend fun setEnabled(moduleId: String, enabled: Boolean)

    /**
     * Пользовательский порядок модулей (идентификаторы).
     *
     * Пустой список — порядок ещё не задавали, работает [app.vdvoem.core.domain.module.ModuleManifest.order].
     * Лишние идентификаторы игнорируются, модули, которых в списке нет, идут следом
     * в порядке манифеста.
     */
    val order: Flow<List<String>>

    suspend fun setOrder(ids: List<String>)

    /** Настройки модулей: id модуля → JSON. Модулей без сохранённых настроек в ответе нет. */
    val settings: Flow<Map<String, String>>

    suspend fun setSettings(moduleId: String, json: String)
}