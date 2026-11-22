package app.vdvoem.core.domain.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Все цветовые роли Material 3, которые можно перекрасить вручную.
 * Значение в карте — ARGB (`0xAARRGGBB`), применяется поверх базовой схемы
 * (брендовой или Material You) отдельно для светлой и тёмной темы.
 */
@Serializable
enum class ColorRole {
    PRIMARY, ON_PRIMARY, PRIMARY_CONTAINER, ON_PRIMARY_CONTAINER, INVERSE_PRIMARY,
    SECONDARY, ON_SECONDARY, SECONDARY_CONTAINER, ON_SECONDARY_CONTAINER,
    TERTIARY, ON_TERTIARY, TERTIARY_CONTAINER, ON_TERTIARY_CONTAINER,
    BACKGROUND, ON_BACKGROUND,
    SURFACE, ON_SURFACE, SURFACE_VARIANT, ON_SURFACE_VARIANT, SURFACE_TINT,
    INVERSE_SURFACE, INVERSE_ON_SURFACE,
    ERROR, ON_ERROR, ERROR_CONTAINER, ON_ERROR_CONTAINER,
    OUTLINE, OUTLINE_VARIANT, SCRIM,
    SURFACE_BRIGHT, SURFACE_DIM,
    SURFACE_CONTAINER, SURFACE_CONTAINER_HIGH, SURFACE_CONTAINER_HIGHEST,
    SURFACE_CONTAINER_LOW, SURFACE_CONTAINER_LOWEST
}

/** Скругления форм Material 3; брендовые значения по умолчанию: 8/12/18/24/32 dp. */
@Serializable
enum class CornerRole { EXTRA_SMALL, SMALL, MEDIUM, LARGE, EXTRA_LARGE }

/** Семантические отступы (токены `VdvoemSpacing`). */
@Serializable
enum class SpacingRole {
    XXS, XS, SM, MD, LG, XL, XXL, XXXL, HUGE, SCREEN_HORIZONTAL, LIST_BOTTOM
}

/** Семантические размеры (токены `VdvoemSizes`). */
@Serializable
enum class SizeRole {
    ICON_SM, ICON_MD, ICON_LG,
    AVATAR_SM, AVATAR_MD, AVATAR_LG, AVATAR_XL,
    TOUCH_TARGET, ROUND_BUTTON, DOT, TIME_COLUMN, SHEET_MAX,
    AVATAR_ACCOUNT, BORDER_THIN, WIDGET_WIDTH, WIDGET_HEIGHT, HERO_BADGE
}

/** Все стили типографики Material 3. */
@Serializable
enum class TypeRole {
    DISPLAY_LARGE, DISPLAY_MEDIUM, DISPLAY_SMALL,
    HEADLINE_LARGE, HEADLINE_MEDIUM, HEADLINE_SMALL,
    TITLE_LARGE, TITLE_MEDIUM, TITLE_SMALL,
    BODY_LARGE, BODY_MEDIUM, BODY_SMALL,
    LABEL_LARGE, LABEL_MEDIUM, LABEL_SMALL
}

/** Токены высот `VdvoemElevation`. */
@Serializable
enum class ElevationRole { CARD, FAB, DIALOG, SHEET }

/**
 * Тонкая настройка оформления: «каждый пиксель» вручную.
 *
 * Пустая карта = роль не тронута и берётся из базовой темы. Семантика:
 * - [lightColors]/[darkColors] — ARGB-перекрытия ролей поверх базовой схемы;
 * - [corners] — радиусы в dp (по умолчанию 8/12/18/24/32);
 * - [spacing] — итоговый dp = (перекрытие ?: базовый токен) × [spacingScale];
 * - [sizes] — итоговый dp = перекрытие ?: базовый токен;
 * - [typeSizes] — итоговый sp = перекрытие ?: (базовый × [fontScale]),
 *   lineHeight масштабируется пропорционально изменению fontSize;
 * - [typeWeights] — насыщенность шрифта (FontWeight 100..900) по стилям;
 * - [typeLetterSpacing] — межбуквенный интервал в sp по стилям;
 * - [elevation] — итоговый dp высоты = (перекрытие ?: базовый токен) × [elevationScale].
 *
 * Диапазоны (UI и движок обязаны их держать): spacingScale 0.5..2,
 * fontScale 0.75..1.5, elevationScale 0..2, corners 0..48 dp,
 * typeSizes 8..64 sp, typeWeights 100..900, typeLetterSpacing -2..4 sp,
 * elevation 0..24 dp, прочие dp-значения >= 0.
 */
@Serializable
data class AppearanceSettings(
    val lightColors: Map<ColorRole, Long> = emptyMap(),
    val darkColors: Map<ColorRole, Long> = emptyMap(),
    val corners: Map<CornerRole, Float> = emptyMap(),
    val spacingScale: Float = 1f,
    val spacing: Map<SpacingRole, Float> = emptyMap(),
    val sizes: Map<SizeRole, Float> = emptyMap(),
    val fontScale: Float = 1f,
    val typeSizes: Map<TypeRole, Float> = emptyMap(),
    val typeWeights: Map<TypeRole, Int> = emptyMap(),
    val typeLetterSpacing: Map<TypeRole, Float> = emptyMap(),
    val elevationScale: Float = 1f,
    val elevation: Map<ElevationRole, Float> = emptyMap()
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val appearance: AppearanceSettings = AppearanceSettings()
)

interface AppSettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setAppearance(appearance: AppearanceSettings)
}