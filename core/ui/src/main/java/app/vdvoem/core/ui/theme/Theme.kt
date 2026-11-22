package app.vdvoem.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vdvoem.core.domain.settings.AppearanceSettings
import app.vdvoem.core.domain.settings.ColorRole
import app.vdvoem.core.domain.settings.CornerRole
import app.vdvoem.core.domain.settings.ElevationRole
import app.vdvoem.core.domain.settings.SizeRole
import app.vdvoem.core.domain.settings.SpacingRole
import app.vdvoem.core.domain.settings.TypeRole

/**
 * Тема приложения.
 *
 * По умолчанию — брендовая палитра (у продукта есть лицо). Dynamic color включается
 * опцией в настройках. Поверх базовой схемы применяются ручные перекрытия
 * [AppearanceSettings] — «каждый пиксель»: цвета, скругления, отступы, размеры, шрифты, высоты.
 */
@Composable
fun VdvoemTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    appearance: AppearanceSettings = AppearanceSettings(),
    content: @Composable () -> Unit
) {
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> VdvoemDarkColors
        else -> VdvoemLightColors
    }
    val colorScheme = baseScheme.withOverrides(if (darkTheme) appearance.darkColors else appearance.lightColors)

    val spacingScale = appearance.spacingScale.coerceIn(0.5f, 2f)
    val baseSpacing = SpacingTokens()
    val spacing = SpacingTokens(
        xxs = appearance.spacingDp(SpacingRole.XXS, baseSpacing.xxs, spacingScale),
        xs = appearance.spacingDp(SpacingRole.XS, baseSpacing.xs, spacingScale),
        sm = appearance.spacingDp(SpacingRole.SM, baseSpacing.sm, spacingScale),
        md = appearance.spacingDp(SpacingRole.MD, baseSpacing.md, spacingScale),
        lg = appearance.spacingDp(SpacingRole.LG, baseSpacing.lg, spacingScale),
        xl = appearance.spacingDp(SpacingRole.XL, baseSpacing.xl, spacingScale),
        xxl = appearance.spacingDp(SpacingRole.XXL, baseSpacing.xxl, spacingScale),
        xxxl = appearance.spacingDp(SpacingRole.XXXL, baseSpacing.xxxl, spacingScale),
        huge = appearance.spacingDp(SpacingRole.HUGE, baseSpacing.huge, spacingScale),
        screenHorizontal = appearance.spacingDp(SpacingRole.SCREEN_HORIZONTAL, baseSpacing.screenHorizontal, spacingScale),
        listBottom = appearance.spacingDp(SpacingRole.LIST_BOTTOM, baseSpacing.listBottom, spacingScale)
    )

    val baseSizes = SizeTokens()
    val sizes = SizeTokens(
        iconSm = appearance.sizeDp(SizeRole.ICON_SM, baseSizes.iconSm),
        iconMd = appearance.sizeDp(SizeRole.ICON_MD, baseSizes.iconMd),
        iconLg = appearance.sizeDp(SizeRole.ICON_LG, baseSizes.iconLg),
        avatarSm = appearance.sizeDp(SizeRole.AVATAR_SM, baseSizes.avatarSm),
        avatarMd = appearance.sizeDp(SizeRole.AVATAR_MD, baseSizes.avatarMd),
        avatarLg = appearance.sizeDp(SizeRole.AVATAR_LG, baseSizes.avatarLg),
        avatarXl = appearance.sizeDp(SizeRole.AVATAR_XL, baseSizes.avatarXl),
        touchTarget = appearance.sizeDp(SizeRole.TOUCH_TARGET, baseSizes.touchTarget),
        roundButton = appearance.sizeDp(SizeRole.ROUND_BUTTON, baseSizes.roundButton),
        dot = appearance.sizeDp(SizeRole.DOT, baseSizes.dot),
        timeColumn = appearance.sizeDp(SizeRole.TIME_COLUMN, baseSizes.timeColumn),
        sheetMax = appearance.sizeDp(SizeRole.SHEET_MAX, baseSizes.sheetMax),
        avatarAccount = appearance.sizeDp(SizeRole.AVATAR_ACCOUNT, baseSizes.avatarAccount),
        borderThin = appearance.sizeDp(SizeRole.BORDER_THIN, baseSizes.borderThin),
        widgetWidth = appearance.sizeDp(SizeRole.WIDGET_WIDTH, baseSizes.widgetWidth),
        widgetHeight = appearance.sizeDp(SizeRole.WIDGET_HEIGHT, baseSizes.widgetHeight),
        heroBadge = appearance.sizeDp(SizeRole.HERO_BADGE, baseSizes.heroBadge)
    )

    val elevationScale = appearance.elevationScale.coerceIn(0f, 2f)
    val baseElevation = ElevationTokens()
    val elevation = ElevationTokens(
        card = appearance.elevationDp(ElevationRole.CARD, baseElevation.card, elevationScale),
        fab = appearance.elevationDp(ElevationRole.FAB, baseElevation.fab, elevationScale),
        dialog = appearance.elevationDp(ElevationRole.DIALOG, baseElevation.dialog, elevationScale),
        sheet = appearance.elevationDp(ElevationRole.SHEET, baseElevation.sheet, elevationScale)
    )

    CompositionLocalProvider(
        LocalSpacingTokens provides spacing,
        LocalSizeTokens provides sizes,
        LocalElevationTokens provides elevation
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = appearanceTypography(appearance),
            shapes = appearanceShapes(appearance.corners),
            content = content
        )
    }
}

/** Итоговый dp отступа: (перекрытие ?: базовый токен) × масштаб, не меньше нуля. */
private fun AppearanceSettings.spacingDp(role: SpacingRole, base: Dp, scale: Float): Dp =
    ((spacing[role] ?: base.value) * scale).coerceAtLeast(0f).dp

/** Итоговый dp размера: перекрытие ?: базовый токен, не меньше нуля. */
private fun AppearanceSettings.sizeDp(role: SizeRole, base: Dp): Dp =
    (sizes[role] ?: base.value).coerceAtLeast(0f).dp

/** Итоговый dp высоты: (перекрытие ?: базовый токен) × масштаб, не меньше нуля. */
private fun AppearanceSettings.elevationDp(role: ElevationRole, base: Dp, scale: Float): Dp =
    ((elevation[role] ?: base.value) * scale).coerceAtLeast(0f).dp

/** ARGB-перекрытия поверх базовой схемы; пустая карта — схему не трогаем. */
private fun ColorScheme.withOverrides(overrides: Map<ColorRole, Long>): ColorScheme {
    if (overrides.isEmpty()) return this
    var scheme = this
    overrides.forEach { (role, argb) ->
        val color = Color(argb)
        scheme = when (role) {
            ColorRole.PRIMARY -> scheme.copy(primary = color)
            ColorRole.ON_PRIMARY -> scheme.copy(onPrimary = color)
            ColorRole.PRIMARY_CONTAINER -> scheme.copy(primaryContainer = color)
            ColorRole.ON_PRIMARY_CONTAINER -> scheme.copy(onPrimaryContainer = color)
            ColorRole.INVERSE_PRIMARY -> scheme.copy(inversePrimary = color)
            ColorRole.SECONDARY -> scheme.copy(secondary = color)
            ColorRole.ON_SECONDARY -> scheme.copy(onSecondary = color)
            ColorRole.SECONDARY_CONTAINER -> scheme.copy(secondaryContainer = color)
            ColorRole.ON_SECONDARY_CONTAINER -> scheme.copy(onSecondaryContainer = color)
            ColorRole.TERTIARY -> scheme.copy(tertiary = color)
            ColorRole.ON_TERTIARY -> scheme.copy(onTertiary = color)
            ColorRole.TERTIARY_CONTAINER -> scheme.copy(tertiaryContainer = color)
            ColorRole.ON_TERTIARY_CONTAINER -> scheme.copy(onTertiaryContainer = color)
            ColorRole.BACKGROUND -> scheme.copy(background = color)
            ColorRole.ON_BACKGROUND -> scheme.copy(onBackground = color)
            ColorRole.SURFACE -> scheme.copy(surface = color)
            ColorRole.ON_SURFACE -> scheme.copy(onSurface = color)
            ColorRole.SURFACE_VARIANT -> scheme.copy(surfaceVariant = color)
            ColorRole.ON_SURFACE_VARIANT -> scheme.copy(onSurfaceVariant = color)
            ColorRole.SURFACE_TINT -> scheme.copy(surfaceTint = color)
            ColorRole.INVERSE_SURFACE -> scheme.copy(inverseSurface = color)
            ColorRole.INVERSE_ON_SURFACE -> scheme.copy(inverseOnSurface = color)
            ColorRole.ERROR -> scheme.copy(error = color)
            ColorRole.ON_ERROR -> scheme.copy(onError = color)
            ColorRole.ERROR_CONTAINER -> scheme.copy(errorContainer = color)
            ColorRole.ON_ERROR_CONTAINER -> scheme.copy(onErrorContainer = color)
            ColorRole.OUTLINE -> scheme.copy(outline = color)
            ColorRole.OUTLINE_VARIANT -> scheme.copy(outlineVariant = color)
            ColorRole.SCRIM -> scheme.copy(scrim = color)
            ColorRole.SURFACE_BRIGHT -> scheme.copy(surfaceBright = color)
            ColorRole.SURFACE_DIM -> scheme.copy(surfaceDim = color)
            ColorRole.SURFACE_CONTAINER -> scheme.copy(surfaceContainer = color)
            ColorRole.SURFACE_CONTAINER_HIGH -> scheme.copy(surfaceContainerHigh = color)
            ColorRole.SURFACE_CONTAINER_HIGHEST -> scheme.copy(surfaceContainerHighest = color)
            ColorRole.SURFACE_CONTAINER_LOW -> scheme.copy(surfaceContainerLow = color)
            ColorRole.SURFACE_CONTAINER_LOWEST -> scheme.copy(surfaceContainerLowest = color)
        }
    }
    return scheme
}

/** Скругления из настроек; брендовые дефолты 8/12/18/24/32 dp, диапазон 0..48. */
private fun appearanceShapes(corners: Map<CornerRole, Float>): Shapes {
    fun corner(role: CornerRole, default: Float) =
        RoundedCornerShape((corners[role] ?: default).coerceIn(0f, 48f).dp)
    return Shapes(
        extraSmall = corner(CornerRole.EXTRA_SMALL, 8f),
        small = corner(CornerRole.SMALL, 12f),
        medium = corner(CornerRole.MEDIUM, 18f),
        large = corner(CornerRole.LARGE, 24f),
        extraLarge = corner(CornerRole.EXTRA_LARGE, 32f)
    )
}

/**
 * Типографика: размер sp = перекрытие ?: базовый × fontScale (lineHeight пропорционально),
 * насыщенность и межбуквенный интервал — перекрытия по стилям поверх базовых.
 */
private fun appearanceTypography(appearance: AppearanceSettings): Typography {
    val base = VdvoemTypography
    fun scaled(style: TextStyle, role: TypeRole): TextStyle {
        val fontScale = appearance.fontScale.coerceIn(0.75f, 1.5f)
        val override = appearance.typeSizes[role]
        val fontSize = if (override != null) override.coerceIn(8f, 64f).sp else style.fontSize * fontScale
        val ratio = if (style.fontSize.value > 0f) fontSize.value / style.fontSize.value else 1f
        val lineHeight = if (style.lineHeight.isSp) (style.lineHeight.value * ratio).sp else style.lineHeight
        val fontWeight = appearance.typeWeights[role]?.coerceIn(100, 900)?.let { FontWeight(it) }
            ?: style.fontWeight
        val letterSpacing = appearance.typeLetterSpacing[role]?.coerceIn(-2f, 4f)?.sp
            ?: style.letterSpacing
        return style.copy(
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontWeight = fontWeight,
            letterSpacing = letterSpacing
        )
    }
    return Typography(
        displayLarge = scaled(base.displayLarge, TypeRole.DISPLAY_LARGE),
        displayMedium = scaled(base.displayMedium, TypeRole.DISPLAY_MEDIUM),
        displaySmall = scaled(base.displaySmall, TypeRole.DISPLAY_SMALL),
        headlineLarge = scaled(base.headlineLarge, TypeRole.HEADLINE_LARGE),
        headlineMedium = scaled(base.headlineMedium, TypeRole.HEADLINE_MEDIUM),
        headlineSmall = scaled(base.headlineSmall, TypeRole.HEADLINE_SMALL),
        titleLarge = scaled(base.titleLarge, TypeRole.TITLE_LARGE),
        titleMedium = scaled(base.titleMedium, TypeRole.TITLE_MEDIUM),
        titleSmall = scaled(base.titleSmall, TypeRole.TITLE_SMALL),
        bodyLarge = scaled(base.bodyLarge, TypeRole.BODY_LARGE),
        bodyMedium = scaled(base.bodyMedium, TypeRole.BODY_MEDIUM),
        bodySmall = scaled(base.bodySmall, TypeRole.BODY_SMALL),
        labelLarge = scaled(base.labelLarge, TypeRole.LABEL_LARGE),
        labelMedium = scaled(base.labelMedium, TypeRole.LABEL_MEDIUM),
        labelSmall = scaled(base.labelSmall, TypeRole.LABEL_SMALL)
    )
}