package app.vdvoem.core.domain.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Работа с датами в одном месте: экраны не должны знать про UTC-полуночи DatePicker
 * и пересчёт годовщин. Дата без времени хранится как UTC-полночь и не зависит от пояса.
 */
object Dates {

    private val dateFormat: DateTimeFormatter
        get() = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())

    fun toLocalDate(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC).toLocalDate()

    fun startOfDay(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun today(): LocalDate = LocalDate.now()

    /** Ближайшее наступление события: для годовщин — этот год или следующий. */
    fun nextOccurrence(epochMillis: Long, annual: Boolean, today: LocalDate = today()): LocalDate {
        val date = toLocalDate(epochMillis)
        if (!annual) return date
        val thisYear = date.withYear(today.year)
        return if (thisYear.isBefore(today)) date.withYear(today.year + 1) else thisYear
    }

    /** Прошедшее наступление события: для годовщин — этот год или предыдущий. */
    fun previousOccurrence(epochMillis: Long, annual: Boolean, today: LocalDate = today()): LocalDate {
        val date = toLocalDate(epochMillis)
        if (!annual) return date
        val thisYear = date.withYear(today.year)
        return if (thisYear.isAfter(today)) date.withYear(today.year - 1) else thisYear
    }

    fun daysUntil(target: LocalDate, from: LocalDate = today()): Long =
        ChronoUnit.DAYS.between(from, target)

    fun format(date: LocalDate): String = date.format(dateFormat)

    /** DatePicker отдаёт полночь по UTC — переводим в локальную полночь и обратно. */
    fun fromPickerMillis(utcMillis: Long): Long {
        val date = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
        return startOfDay(date)
    }

    fun toPickerMillis(epochMillis: Long): Long {
        val date = toLocalDate(epochMillis)
        return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
}
