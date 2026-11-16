package app.vdvoem.feature.voice.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

private val Context.voicePreferences by preferencesDataStore(name = VoiceNoteStore.STORE_NAME)

/**
 * Локальное хранилище голосовых заметок: аудиофайлы лежат в [DIR_NAME] внутри
 * `filesDir`, метаданные — одной JSON-строкой в DataStore.
 *
 * Заметки не синхронизируются: аудио остаётся на устройстве, поэтому модуль
 * объявлен как [app.vdvoem.core.domain.module.ModuleScope.LOCAL].
 */
class VoiceNoteStore(private val context: Context) {

    /** Каталог с аудиофайлами; создаётся при обращении. */
    val directory: File
        get() = File(context.filesDir, DIR_NAME).apply { mkdirs() }

    val notes: Flow<List<VoiceNote>> = context.voicePreferences.data
        .catch { throwable ->
            if (throwable is IOException) {
                Log.w(TAG, "Не удалось прочитать голосовые заметки", throwable)
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences -> parse(preferences[notesKey]) }

    /** Добавляет заметку в начало списка. */
    suspend fun save(note: VoiceNote) {
        context.voicePreferences.edit { preferences ->
            val updated = (parse(preferences[notesKey]) + note).sortedByDescending { it.createdAt }
            preferences[notesKey] = serialize(updated)
        }
    }

    /** Удаляет заметку и её аудиофайл. */
    suspend fun delete(note: VoiceNote) {
        context.voicePreferences.edit { preferences ->
            val updated = parse(preferences[notesKey]).filterNot { it.id == note.id }
            preferences[notesKey] = serialize(updated)
        }
        runCatching { File(note.filePath).delete() }
    }

    /** Новый целевой файл для записи; имя уникально. */
    fun newFile(): File = File(directory, "voice_${System.currentTimeMillis()}_${System.nanoTime() % 1000}.m4a")

    private fun parse(raw: String?): List<VoiceNote> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val id = item.optString(KEY_ID)
                val path = item.optString(KEY_PATH)
                if (id.isBlank() || path.isBlank()) return@mapNotNull null
                VoiceNote(
                    id = id,
                    title = item.optString(KEY_TITLE),
                    filePath = path,
                    durationMs = item.optLong(KEY_DURATION, 0L),
                    createdAt = item.optLong(KEY_CREATED, 0L)
                )
            }
        }.getOrElse { throwable ->
            Log.w(TAG, "Повреждённые метаданные голосовых заметок", throwable)
            emptyList()
        }
    }

    private fun serialize(notes: List<VoiceNote>): String {
        val array = JSONArray()
        notes.forEach { note ->
            array.put(
                JSONObject()
                    .put(KEY_ID, note.id)
                    .put(KEY_TITLE, note.title)
                    .put(KEY_PATH, note.filePath)
                    .put(KEY_DURATION, note.durationMs)
                    .put(KEY_CREATED, note.createdAt)
            )
        }
        return array.toString()
    }

    companion object {
        const val STORE_NAME = "voice_notes"
        const val DIR_NAME = "voice"

        private const val TAG = "Voice"
        private val notesKey = stringPreferencesKey("notes_json")
        private const val KEY_ID = "id"
        private const val KEY_TITLE = "title"
        private const val KEY_PATH = "path"
        private const val KEY_DURATION = "duration"
        private const val KEY_CREATED = "createdAt"
    }
}