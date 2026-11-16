package app.vdvoem.core.domain.module

import androidx.annotation.StringRes

/** Группа, в которой модуль показывается в магазине дополнений. */
enum class ModuleCategory {
    CORE,
    TOGETHER,
    PERSONAL,
    FUN
}

/**
 * Область жизни данных модуля.
 *
 * - [SHARED] — данные принадлежат паре и синхронизируются с партнёром (календарь, желания,
 *   подарки, викторины). Ценность модуля именно в обмене.
 * - [LOCAL] — данные живут только на этом устройстве и никогда не уезжают на сервер
 *   (например, личные заметки или таймеры). Синхронизация их игнорирует.
 *
 * Область — свойство модуля, а не пользовательская настройка: пользователь выбирает
 * лишь видимость модуля в навигации (см. [app.vdvoem.core.domain.repository.ModuleSettingsRepository]).
 */
enum class ModuleScope {
    SHARED,
    LOCAL
}

/**
 * Описание модуля для реестра и магазина дополнений.
 *
 * Все тексты — ресурсы, чтобы модуль оставался переводимым.
 *
 * Модули больше не включаются и не выключаются: всё, что собрано в APK, доступно всегда.
 * Пользователь может только скрыть модуль из навигации — данные при этом сохраняются.
 */
data class ModuleManifest(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val category: ModuleCategory,
    val order: Int,
    /**
     * Оставлено для совместимости исходников: раньше означало «включён по умолчанию».
     * Сейчас на фильтрацию не влияет — модуль доступен, пока его не скрыли.
     */
    val defaultEnabled: Boolean = false,
    val requiresNetwork: Boolean = false,
    val comingSoon: Boolean = false,
    /** Короткое имя для нижней навигации; 0 — использовать [titleRes]. */
    @StringRes val shortTitleRes: Int = 0,
    /**
     * Область данных модуля: [ModuleScope.SHARED] — синхронизируется с партнёром,
     * [ModuleScope.LOCAL] — живёт только на этом устройстве. По умолчанию — общая.
     */
    val scope: ModuleScope = ModuleScope.SHARED
)