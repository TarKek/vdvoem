package app.vdvoem.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Системный ресивер виджета «Цикл».
 *
 * `glanceAppWidget` — единственное, что нужно Glance: он сам строит RemoteViews
 * и обрабатывает добавление/удаление/изменение размера.
 */
class CycleWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = CycleWidget()

    /**
     * Система присылает APPWIDGET_UPDATE при установке, перезагрузке и добавлении
     * виджета. `super` перерисовывает все экземпляры через Glance; данные при этом
     * читаются заново, потому что `provideGlance` каждый раз ходит в репозиторий.
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }
}