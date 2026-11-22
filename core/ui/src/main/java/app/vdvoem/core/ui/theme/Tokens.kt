package app.vdvoem.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Семантические токены размеров. Экраны не используют «сырые» dp — только эти значения
 * (и размеры из MaterialTheme), чтобы редизайн менялся в одном месте.
 *
 * Значения по умолчанию — бренд; пользователь может перекрыть каждый токен в настройках
 * оформления ([app.vdvoem.core.domain.settings.AppearanceSettings]), поэтому токены
 * читаются через CompositionLocal, а не как константы.
 */
data class SpacingTokens(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp,
    val huge: Dp = 40.dp,

    /** Горизонтальные поля экранов. */
    val screenHorizontal: Dp = 20.dp,

    /** Нижний отступ списков, чтобы FAB/панель не перекрывали контент. */
    val listBottom: Dp = 96.dp
)

data class SizeTokens(
    val iconSm: Dp = 16.dp,
    val iconMd: Dp = 20.dp,
    val iconLg: Dp = 24.dp,
    val avatarSm: Dp = 32.dp,
    val avatarMd: Dp = 40.dp,
    val avatarLg: Dp = 56.dp,
    val avatarXl: Dp = 76.dp,

    /** Минимальная доступная область нажатия. */
    val touchTarget: Dp = 48.dp,

    /** Маленькая круглая кнопка поверх карточки («Сегодня»). */
    val roundButton: Dp = 40.dp,

    /** Точка-намёк на кнопке «Сегодня»: есть событие календаря. */
    val dot: Dp = 8.dp,

    /** Колонка времени в списке уроков. */
    val timeColumn: Dp = 76.dp,

    /** Предельная высота нижней шторки: дальше — прокрутка, а не экран во весь рост. */
    val sheetMax: Dp = 560.dp,

    /** Круглая кнопка аккаунта пары в шапке главного экрана. */
    val avatarAccount: Dp = 36.dp,

    /** Тонкая обводка: аватары, разделители. */
    val borderThin: Dp = 1.dp,
    val widgetWidth: Dp = 168.dp,
    val widgetHeight: Dp = 128.dp,
    val heroBadge: Dp = 44.dp
)

/** Высоты (тени) компонентов; умножаются на elevationScale из настроек оформления. */
data class ElevationTokens(
    val card: Dp = 0.dp,
    val fab: Dp = 6.dp,
    val dialog: Dp = 6.dp,
    val sheet: Dp = 1.dp
)

internal val LocalSpacingTokens = staticCompositionLocalOf { SpacingTokens() }
internal val LocalSizeTokens = staticCompositionLocalOf { SizeTokens() }
internal val LocalElevationTokens = staticCompositionLocalOf { ElevationTokens() }

/** Вызовы вида `VdvoemSpacing.xxs` работают внутри @Composable как раньше. */
val VdvoemSpacing: SpacingTokens
    @Composable get() = LocalSpacingTokens.current

val VdvoemSizes: SizeTokens
    @Composable get() = LocalSizeTokens.current

val VdvoemElevation: ElevationTokens
    @Composable get() = LocalElevationTokens.current