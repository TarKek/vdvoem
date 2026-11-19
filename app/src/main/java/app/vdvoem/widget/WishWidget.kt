package app.vdvoem.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.vdvoem.MainActivity

/** Системный виджет «Желание дня»: одно желание из общего списка пары. */
class WishWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WishWidgetState.load(context)
        provideContent { WishWidgetContent(data) }
    }
}

@Composable
private fun WishWidgetContent(data: WishWidgetData) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(BrandBackground))
            .cornerRadius(24.dp)
            .padding(14.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Vertical.Top,
        horizontalAlignment = Alignment.Horizontal.Start
    ) {
        Text(
            text = data.headerLabel,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = ColorProvider(BrandPrimary)
            ),
            maxLines = 1
        )

        Spacer(modifier = GlanceModifier.size(8.dp))

        if (data.title == null) {
            Text(
                text = data.emptyLabel,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = ColorProvider(BrandMuted)
                ),
                maxLines = 2
            )
        } else {
            Text(
                text = data.title,
                style = TextStyle(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(BrandOnSurface)
                ),
                maxLines = 3
            )

            data.subtitle?.let { subtitle ->
                Spacer(modifier = GlanceModifier.size(6.dp))
                Text(
                    text = subtitle,
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorProvider(BrandPrimary)
                    ),
                    maxLines = 1
                )
            }
        }
    }
}