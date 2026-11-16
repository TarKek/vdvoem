package app.vdvoem.core.domain.module

/**
 * Реестр всех собранных в APK модулей.
 *
 * Базовый порядок задаётся [ModuleManifest.order], поэтому видимые модули в нижней навигации
 * идут в предсказуемом порядке независимо от порядка регистрации в DI. Пользователь может
 * переставить модули — тогда используется сохранённый порядок (см. [ordered]).
 *
 * Модули не включаются и не выключаются: всё собранное доступно всегда. Пользователь может
 * лишь скрыть модуль из навигации — см. [visible].
 */
class ModuleRegistry(modules: List<AppModule>) {

    val modules: List<AppModule> = modules.sortedBy { it.manifest.order }

    private val byRoute: Map<String, AppModule> = this.modules.associateBy { it.route }

    private val byId: Map<String, AppModule> = this.modules.associateBy { it.manifest.id }

    /**
     * Устаревший набор «включённых по умолчанию» из манифестов.
     *
     * Оставлен для совместимости исходников; навигация его больше не использует.
     */
    val defaultEnabledIds: Set<String> =
        modules.filter { it.manifest.defaultEnabled }.map { it.manifest.id }.toSet()

    /** Модули, которые можно показывать и скрывать. */
    val toggleable: List<AppModule> = this.modules.filter { !it.manifest.comingSoon }

    /**
     * Модули в пользовательском порядке: сначала известные из [orderIds] (в сохранённой
     * последовательности), затем остальные по [ModuleManifest.order].
     *
     * Неизвестные и повторяющиеся идентификаторы игнорируются, поэтому устаревшие настройки
     * или удалённые модули ничего не ломают.
     */
    fun ordered(orderIds: List<String>): List<AppModule> {
        if (orderIds.isEmpty()) return modules
        val known = orderIds.distinct().mapNotNull(byId::get)
        if (known.isEmpty()) return modules
        val knownIds = known.mapTo(HashSet()) { it.manifest.id }
        return known + modules.filterNot { it.manifest.id in knownIds }
    }

    /** Эффективный порядок идентификаторов всех модулей — его и сохраняем в настройках. */
    fun effectiveOrder(orderIds: List<String>): List<String> =
        ordered(orderIds).map { it.manifest.id }

    /**
     * Модули, которые пользователь не скрыл: [modules] без тех, чей id попал в [hidden].
     *
     * Порядок входного списка сохраняется, поэтому сюда обычно передают результат [ordered].
     * Скрытие — только про навигацию: данные скрытого модуля остаются на месте.
     */
    fun visible(modules: List<AppModule>, hidden: Set<String>): List<AppModule> =
        if (hidden.isEmpty()) modules else modules.filterNot { it.manifest.id in hidden }

    fun byRoute(route: String?): AppModule? = route?.let(byRoute::get)

    fun byId(id: String): AppModule? = byId[id]

    /** Устаревшая проверка включённости; см. [visible]. */
    fun isEnabled(module: AppModule, enabledIds: Set<String>): Boolean =
        module.manifest.id in enabledIds
}