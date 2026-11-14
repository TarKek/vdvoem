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
 * Описание модуля для реестра и магазина дополнений.
 *
 * Все тексты — ресурсы, чтобы модуль оставался переводимым.
 */
data class ModuleManifest(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val category: ModuleCategory,
    val order: Int,
    val defaultEnabled: Boolean = false,
    val requiresNetwork: Boolean = false,
    val comingSoon: Boolean = false
)
