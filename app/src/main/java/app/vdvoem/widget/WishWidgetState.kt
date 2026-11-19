package app.vdvoem.widget

import android.content.Context
import app.vdvoem.R
import app.vdvoem.core.domain.device.DeviceIdentity
import app.vdvoem.core.domain.model.Wish
import app.vdvoem.core.domain.model.WishStatus
import app.vdvoem.core.domain.repository.WishRepository
import app.vdvoem.core.domain.time.Dates
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext

/** Готовый к отрисовке снимок виджета «Желание дня». */
internal data class WishWidgetData(
    val headerLabel: String,
    /** `null`, когда желаний нет: тогда показываем только подсказку. */
    val title: String?,
    val subtitle: String?,
    val emptyLabel: String
)

/**
 * Выбирает желание дня из общего списка пары.
 *
 * Данные читаются так же, как у остальных виджетов: из общего Koin-контейнера.
 * Если контейнер недоступен — не падаем, а показываем empty state.
 */
internal object WishWidgetState {

    suspend fun load(context: Context): WishWidgetData {
        val koin = runCatching { GlobalContext.get() }.getOrNull()
        val wishes = runCatching {
            koin?.get<WishRepository>()?.observeWishes()?.first()
        }.getOrNull().orEmpty()
        val myDeviceId = runCatching { koin?.get<DeviceIdentity>()?.deviceId() }.getOrNull().orEmpty()

        val wish = wishOfDay(wishes, Dates.today(), myDeviceId)

        return WishWidgetData(
            headerLabel = context.getString(R.string.widget_wish_label),
            title = wish?.title,
            subtitle = wish?.let { subtitle(context, it) },
            emptyLabel = context.getString(R.string.widget_wish_empty)
        )
    }

    private fun subtitle(context: Context, wish: Wish): String? = when {
        wish.status == WishStatus.RESERVED -> context.getString(R.string.widget_wish_reserved)
        wish.safePriority == 2 -> context.getString(R.string.widget_wish_high)
        else -> null
    }
}