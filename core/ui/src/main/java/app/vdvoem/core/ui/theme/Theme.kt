package app.vdvoem.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Тема приложения.
 *
 * По умолчанию — брендовая палитра (у продукта есть лицо). Dynamic color включается
 * опцией в настройках, когда она появится: параметр уже есть.
 */
@Composable
fun VdvoemTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> VdvoemDarkColors
        else -> VdvoemLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VdvoemTypography,
        shapes = VdvoemShapes,
        content = content
    )
}
