package app.vdvoem.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Единая точка обновления системного виджета.
 *
 * Glance перерисовывает виджет только по системному событию (`updatePeriodMillis = 0`,
 * то есть сам по себе он не обновляется). Данные цикла меняются внутри приложения,
 * поэтому после правки нужно позвать [requestRefresh].
 */
object CycleWidgetUpdater {

    // Свой scope на процесс: виджет живёт дольше любой активити, а держать корутину
    // в GlobalScope нельзя (утечки и неотменяемая работа).
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Перерисовать все экземпляры виджета по текущим данным. */
    suspend fun refresh(context: Context) {
        CycleWidget().updateAll(context.applicationContext)
    }

    /** Неблокирующий вариант для UI-кода и ресиверов. */
    fun requestRefresh(context: Context) {
        val appContext = context.applicationContext
        scope.launch { runCatching { refresh(appContext) } }
    }
}