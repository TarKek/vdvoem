package app.vdvoem.core.domain.module

/**
 * Реестр всех собранных в APK модулей.
 *
 * Порядок задаётся [ModuleManifest.order], поэтому включённые модули в нижней навигации
 * идут в предсказуемом порядке независимо от порядка регистрации в DI.
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

    fun byRoute(route: String?): AppModule? = route?.let(byRoute::get)

    fun byId(id: String): AppModule? = byId[id]

    fun isEnabled(module: AppModule, enabledIds: Set<String>): Boolean =
        module.manifest.id in enabledIds
}
