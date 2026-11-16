package app.vdvoem.feature.voice.data

/**
 * Голосовая заметка, записанная на телефоне.
 *
 * [filePath] — абсолютный путь к файлу в приватном хранилище приложения
 * (`filesDir/voice/...`), поэтому заметка доступна только на этом устройстве.
 */
data class VoiceNote(
    val id: String,
    val title: String,
    val filePath: String,
    val durationMs: Long,
    val createdAt: Long
) {
    val durationSeconds: Int
        get() = (durationMs / 1000L).toInt()
}