package app.vdvoem.core.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Семантические токены размеров. Экраны не используют «сырые» dp — только эти значения
 * (и размеры из MaterialTheme), чтобы редизайн менялся в одном месте.
 */
object VdvoemSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val huge = 40.dp

    /** Горизонтальные поля экранов. */
    val screenHorizontal = 20.dp

    /** Нижний отступ списков, чтобы FAB/панель не перекрывали контент. */
    val listBottom = 96.dp
}

object VdvoemSizes {
    val iconSm = 16.dp
    val iconMd = 20.dp
    val iconLg = 24.dp
    val avatarSm = 32.dp
    val avatarMd = 40.dp
    val avatarLg = 56.dp
    val avatarXl = 76.dp

    /** Минимальная доступная область нажатия. */
    val touchTarget = 48.dp

    /** Круглая кнопка аккаунта пары в шапке главного экрана. */
    val avatarAccount = 36.dp

    /** Тонкая обводка: аватары, разделители. */
    val borderThin = 1.dp
    val widgetWidth = 168.dp
    val widgetHeight = 128.dp
    val heroBadge = 44.dp
}
