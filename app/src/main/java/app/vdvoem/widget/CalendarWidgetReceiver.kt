package app.vdvoem.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Системный ресивер виджета «Календарь».
 *
 * Как и у виджета цикла: Glance сам строит RemoteViews и обрабатывает
 * добавление/удаление/изменение размера, а данные читаются в `provideGlance`.
 */
class CalendarWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = CalendarWidget()
}