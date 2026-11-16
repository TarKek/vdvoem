package app.vdvoem.core.domain.module

/**
 * Реестр всех собранных в APK модулей.
 *
 * Базовый порядок задаётся [ModuleManifest.order], поэтому включённые модули в нижней навигации
 * идут в предсказуемом порядке независимо от порядка регистрации в DI. Пользователь может
 * переставить модули — тогда используется сохранённый порядок (см. [ordered]).
 */
class ModuleRegistry(modules: List<AppModule>) {

    val modules: List<AppModule> = modules.sortedBy { it.manifest.order }

    private val byRoute: Map<String, AppModule> = this.modules.associateBy { it.route }

    private val byId: Map<String, AppModule> = this.modules.associateBy { it.manifest.id }

    /** Модули, включённые по умолчанию (пока пользователь ничего не менял). */
    val defaultEnabledIds: Set<String> =
        modules.filter { it.manifest.defaultEnabled }.map { it.manifest.id }.toSet()

    /** Модули, которые можно включать и выключать. */
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

    fun byRoute(route: String?): AppModule? = route?.let(byRoute::get)

    fun byId(id: String): AppModule? = byId[id]

    fun isEnabled(module: AppModule, enabledIds: Set<String>): Boolean =
        module.manifest.id in enabledIds
}