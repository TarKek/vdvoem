package app.vdvoem.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Брендовая палитра «Вдвоём»: тёплый розово-сливовый основной + спокойный бирюзовый акцент.
 * Все цвета живут здесь — в экранах используются только роли из MaterialTheme.colorScheme.
 */
internal val VdvoemLightColors = lightColorScheme(
    primary = Color(0xFF8E4A5F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E2),
    onPrimaryContainer = Color(0xFF3B071C),
    secondary = Color(0xFF74565E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFADDE2),
    onSecondaryContainer = Color(0xFF2B151B),
    tertiary = Color(0xFF3F6B63),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC1EFE6),
    onTertiaryContainer = Color(0xFF00201C),
    background = Color(0xFFFFF8F8),
    onBackground = Color(0xFF22191B),
    surface = Color(0xFFFFF8F8),
    onSurface = Color(0xFF22191B),
    surfaceVariant = Color(0xFFF2DDE1),
    onSurfaceVariant = Color(0xFF514347),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF0F2),
    surfaceContainer = Color(0xFFFCEAEC),
    surfaceContainerHigh = Color(0xFFF6E4E6),
    surfaceContainerHighest = Color(0xFFF0DEE0),
    outline = Color(0xFF837377),
    outlineVariant = Color(0xFFD5C2C6),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

internal val VdvoemDarkColors = darkColorScheme(
    primary = Color(0xFFFFB1C5),
    onPrimary = Color(0xFF541F31),
    primaryContainer = Color(0xFF713348),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = Color(0xFFE3BDC5),
    onSecondary = Color(0xFF422930),
    secondaryContainer = Color(0xFF5A3F46),
    onSecondaryContainer = Color(0xFFFFD9E2),
    tertiary = Color(0xFFA5D5CB),
    onTertiary = Color(0xFF0D3E37),
    tertiaryContainer = Color(0xFF254F49),
    onTertiaryContainer = Color(0xFFC1EFE6),
    background = Color(0xFF1A1113),
    onBackground = Color(0xFFF0DEE1),
    surface = Color(0xFF1A1113),
    onSurface = Color(0xFFF0DEE1),
    surfaceVariant = Color(0xFF514347),
    onSurfaceVariant = Color(0xFFD5C2C6),
    surfaceContainerLowest = Color(0xFF140C0E),
    surfaceContainerLow = Color(0xFF22191B),
    surfaceContainer = Color(0xFF271D1F),
    surfaceContainerHigh = Color(0xFF32282A),
    surfaceContainerHighest = Color(0xFF3D3235),
    outline = Color(0xFF9D8C90),
    outlineVariant = Color(0xFF514347),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)
