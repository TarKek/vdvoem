package app.vdvoem.widget

import android.content.Context
import app.vdvoem.R
import app.vdvoem.core.domain.model.DiaryEntry
import app.vdvoem.core.domain.model.DiaryReminder
import app.vdvoem.core.domain.repository.DiaryReminderRepository
import app.vdvoem.core.domain.repository.DiaryRepository
import app.vdvoem.core.domain.time.Dates
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Готовый к отрисовке снимок виджета «Дневник».
 *
 * Все подписи собираются здесь, в композиции виджета остаются только стили:
 * так форматирование даты и времени не зависит от перерисовки Glance.
 */
internal data class DiaryWidgetData(
    val headerLabel: String,
    val dateLabel: String,
    /** Заголовок записи за сегодня; `null`, когда записи нет. */
    val title: String?,
    /** Короткий текст записи под заголовком; `null`, если текста нет. */
    val body: String?,
    val emptyLabel: String,
    /** «12:00 · текст» по первому невыполненному напоминанию на сегодня. */
    val reminderLabel: String?
)

/**
 * Читает запись и напоминания дневника из общего Koin-контейнера.
 *
 * Контейнер уже поднят `VdvoemApplication`; если он недоступен или данных нет,
 * виджет не падает, а показывает empty state — как остальные виджеты.
 */
internal object DiaryWidgetState {

    suspend fun load(context: Context): DiaryWidgetData {
        val today = Dates.today()
        // Дата записи хранится как UTC-полночь — сравниваем с такой же полночью.
        val todayStart = Dates.startOfDay(today)

        val entries = runCatching {
            GlobalContext.get().get<DiaryRepository>().observeEntries().first()
        }.getOrDefault(emptyList())
        val reminders = runCatching {
            GlobalContext.get().get<DiaryReminderRepository>().observeReminders().first()
        }.getOrDefault(emptyList())

        val entry = entries.firstOrNull { it.deletedAt == null && it.dateMillis == todayStart }
        // Напоминание точное (дата + время), поэтому день считаем в поясе устройства.
        val reminder = reminders
            .filter { it.isActive && isToday(it.remindAt, today) }
            .minByOrNull { it.remindAt }

        return DiaryWidgetData(
            headerLabel = context.getString(R.string.widget_diary_label),
            dateLabel = today.format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault())),
            title = entry?.let { title(it) },
            body = entry?.let { body(it) },
            emptyLabel = context.getString(R.string.widget_diary_empty),
            reminderLabel = reminder?.let { label(context, it) }
        )
    }

    /**
     * Заголовок записи. Если своего заголовка нет, берём первую непустую строку
     * текста — так запись без заголовка всё равно читается.
     */
    private fun title(entry: DiaryEntry): String? {
        val own = entry.title.trim()
        if (own.isNotEmpty()) return own
        return lines(entry.body).firstOrNull()
    }

    /** Текст под заголовком: строки, из которых заголовок не взяли. */
    private fun body(entry: DiaryEntry): String? {
        val rows = lines(entry.body)
        val rest = if (entry.title.isBlank()) rows.drop(1) else rows
        return rest.joinToString(" ").trim().ifEmpty { null }
    }

    private fun lines(body: String): List<String> =
        body.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()

    private fun label(context: Context, reminder: DiaryReminder): String = context.getString(
        R.string.widget_diary_reminder,
        Instant.ofEpochMilli(reminder.remindAt)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())),
        reminder.text
    )

    private fun isToday(millis: Long, today: LocalDate): Boolean =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate() == today
}