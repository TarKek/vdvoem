package app.vdvoem.widget

import androidx.compose.ui.graphics.Color

/**
 * Брендовые цвета для Glance-виджетов.
 *
 * MaterialTheme в виджете недоступен (RemoteViews вне Compose), а палитра в
 * `:core:ui` объявлена `internal` и наружу не выходит, поэтому берём те же значения
 * из `core/ui/theme/Color.kt` константами. Меняется палитра — меняем и здесь.
 */
internal val BrandBackground = Color(0xFFFFECEF)
internal val BrandPrimary = Color(0xFF8E4A5F)
internal val BrandOnSurface = Color(0xFF22191B)
internal val BrandMuted = Color(0xFF514347)
internal val BrandTrack = Color(0xFFF2DDE1)
internal val BrandForecast = Color(0xFFFFB1C5)