package app.vdvoem.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Единая точка обновления системных виджетов.
 *
 * Glance перерисовывает виджет только по системному событию, а данные меняются
 * внутри приложения. Поэтому после правок и при возврате в приложение зовём
 * [requestRefresh]: он обновляет все виджеты разом.
 */
object WidgetRefresher {

    // Свой scope на процесс: виджеты живут дольше любой активити, а держать корутину
    // в GlobalScope нельзя (утечки и неотменяемая работа).
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Перерисовать все экземпляры всех виджетов по текущим данным.
     *
     * Каждый виджет обновляем отдельно: сломанный или удалённый виджет не должен
     * мешать остальным.
     */
    suspend fun refresh(context: Context) {
        val appContext = context.applicationContext
        runCatching { CycleWidget().updateAll(appContext) }
        runCatching { CalendarWidget().updateAll(appContext) }
        runCatching { WishWidget().updateAll(appContext) }
        runCatching { DiaryWidget().updateAll(appContext) }
    }

    /** Неблокирующий вариант для UI-кода и ресиверов. */
    fun requestRefresh(context: Context) {
        val appContext = context.applicationContext
        scope.launch { runCatching { refresh(appContext) } }
    }
}