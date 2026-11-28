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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.vdvoem.MainActivity

/** Системный виджет «Дневник»: запись за сегодня и ближайшее напоминание. */
class DiaryWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Данные читаем до композиции: внутри provideContent suspend-чтение недоступно.
        val data = DiaryWidgetState.load(context)
        provideContent { DiaryWidgetContent(data) }
    }
}

@Composable
private fun DiaryWidgetContent(data: DiaryWidgetData) {
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
                text = data.headerLabel,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorProvider(BrandPrimary)
                ),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = data.dateLabel,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = ColorProvider(BrandMuted)
                ),
                maxLines = 1
            )
        }

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
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(BrandOnSurface)
                ),
                maxLines = 2
            )

            data.body?.let { body ->
                Spacer(modifier = GlanceModifier.size(4.dp))
                Text(
                    text = body,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = ColorProvider(BrandMuted)
                    ),
                    maxLines = 2
                )
            }
        }

        // Напоминание прижимаем к низу: оно дополняет запись, а не спорит с ней.
        data.reminderLabel?.let { reminder ->
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = reminder,
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