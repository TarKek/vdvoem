package app.vdvoem.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Состояние модулей: какие скрыты из навигации, в каком порядке идут и их локальные настройки.
 *
 * Модули не включаются и не выключаются: всё, что собрано в APK, доступно всегда. Пользователь
 * решает только, показывать модуль в навигации или скрыть; скрытие не удаляет данные.
 *
 * Хранится локально. Позже сюда же придёт синхронизация, но интерфейс останется тем же,
 * поэтому фичи не знают, откуда берутся настройки.
 */
interface ModuleSettingsRepository {

    /**
     * Идентификаторы модулей, скрытых пользователем из навигации.
     *
     * По умолчанию пусто — видны все модули.
     */
    val hiddenIds: Flow<Set<String>>

    /** Скрыть модуль из навигации ([hidden] = true) или снова показать. */
    suspend fun setHidden(moduleId: String, hidden: Boolean)

    /**
     * Устаревший API включённых модулей.
     *
     * Оставлен для совместимости исходников; интерфейс на него больше не опирается —
     * фильтрация идёт через [hiddenIds].
     */
    val enabledIds: Flow<Set<String>>

    /** Устаревший аналог [setHidden]; см. примечание у [enabledIds]. */
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