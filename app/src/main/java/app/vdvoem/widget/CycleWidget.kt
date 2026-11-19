package app.vdvoem.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.vdvoem.MainActivity

/** Системный виджет «Цикл»: день, фаза, подсказка и пять ближайших дней точками. */
class CycleWidget : GlanceAppWidget() {

    // Размер реальный: одна и та же вёрстка читается и в 2x2, и в 4x2.
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Данные читаем до композиции: Glance не умеет suspend-чтения внутри
        // provideContent, а обновлять виджет из provideGlance нельзя (это рекурсия).
        val data = CycleWidgetState.load(context)
        provideContent { CycleWidgetContent(data) }
    }
}

@Composable
private fun CycleWidgetContent(data: CycleWidgetData) {
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
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = data.dateLabel,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorProvider(BrandMuted)
                ),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            data.phaseLabel?.let { phase ->
                Text(
                    text = phase,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorProvider(BrandPrimary)
                    ),
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = GlanceModifier.size(6.dp))

        data.dayLabel?.let { day ->
            Text(
                text = day,
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(BrandOnSurface)
                ),
                maxLines = 1
            )
        }

        Spacer(modifier = GlanceModifier.defaultWeight())

        Text(
            text = data.hintLabel,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = ColorProvider(BrandMuted)
            ),
            maxLines = 2
        )

        Spacer(modifier = GlanceModifier.size(8.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            data.markers.forEachIndexed { index, marker ->
                if (index > 0) Spacer(modifier = GlanceModifier.width(6.dp))
                Box(
                    modifier = GlanceModifier
                        .size(10.dp)
                        .background(ColorProvider(marker.color()))
                        .cornerRadius(5.dp)
                ) {}
            }
        }
    }
}

private fun CycleDayMarker.color(): Color = when (this) {
    CycleDayMarker.PERIOD -> BrandPrimary
    CycleDayMarker.FORECAST -> BrandForecast
    CycleDayMarker.NONE -> BrandTrack
}