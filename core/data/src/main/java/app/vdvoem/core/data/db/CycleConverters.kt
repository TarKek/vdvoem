package app.vdvoem.core.data.db

import androidx.room.TypeConverter

/**
 * Список симптомов дня цикла в одной колонке.
 *
 * Ключи симптомов стабильные и заранее известные (`cramps`, `headache`, …),
 * разделитель `|` внутри ключа не встречается, поэтому JSON не нужен: строка
 * короче и не требует зависимостей. Пустой список хранится как `""`.
 */
class CycleConverters {

    @TypeConverter
    fun toSymptoms(value: String?): List<String> =
        value?.split(SEPARATOR)?.filter { it.isNotBlank() } ?: emptyList()

    @TypeConverter
    fun fromSymptoms(value: List<String>): String =
        value.filter { it.isNotBlank() }.joinToString(SEPARATOR)

    private companion object {
        const val SEPARATOR = "|"
    }
}