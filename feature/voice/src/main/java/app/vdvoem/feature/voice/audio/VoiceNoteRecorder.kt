package app.vdvoem.feature.voice.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.util.Log
import java.io.File

/** Готовая запись: файл в приватном хранилище и её длительность. */
data class Recording(
    val file: File,
    val durationMs: Long
)

/**
 * Запись голоса в AAC/MP4 через [MediaRecorder].
 *
 * Пишем в кеш (`cacheDir`) и только после успешного [stop] переносим файл в
 * `filesDir/voice`, чтобы недописанные записи не засоряли основное хранилище.
 * Разрешение `RECORD_AUDIO` проверяет экран, рекордер его не запрашивает.
 */
class VoiceNoteRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var tempFile: File? = null
    private var startedAt = 0L

    val isRecording: Boolean
        get() = recorder != null

    /** Стартует запись; `false` — если запись уже идёт или микрофон недоступен. */
    fun start(): Boolean {
        if (recorder != null) return false
        val file = runCatching { File.createTempFile("voice_", ".m4a", context.cacheDir) }
            .getOrElse { throwable ->
                Log.w(TAG, "Не удалось создать временный файл", throwable)
                return false
            }
        val mediaRecorder = createRecorder()
        return try {
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(BIT_RATE)
                setAudioSamplingRate(SAMPLE_RATE)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            tempFile = file
            startedAt = SystemClock.elapsedRealtime()
            true
        } catch (throwable: Throwable) {
            Log.w(TAG, "Не удалось начать запись", throwable)
            runCatching { mediaRecorder.release() }
            runCatching { file.delete() }
            false
        }
    }

    /** Останавливает запись; `null` — если запись пустая или не началась. */
    fun stop(): Recording? {
        val mediaRecorder = recorder ?: return null
        val file = tempFile
        val durationMs = SystemClock.elapsedRealtime() - startedAt
        recorder = null
        tempFile = null
        return try {
            mediaRecorder.stop()
            mediaRecorder.release()
            if (file == null || !file.exists() || file.length() == 0L) {
                file?.delete()
                null
            } else {
                Recording(file = moveToStorage(file), durationMs = durationMs)
            }
        } catch (throwable: Throwable) {
            // MediaRecorder.stop() бросает RuntimeException, если запись слишком короткая.
            Log.w(TAG, "Не удалось корректно завершить запись", throwable)
            runCatching { mediaRecorder.release() }
            file?.delete()
            null
        }
    }

    /** Прерывает запись и удаляет файл. */
    fun cancel() {
        val mediaRecorder = recorder
        recorder = null
        mediaRecorder?.let { active ->
            runCatching { active.stop() }
            runCatching { active.release() }
        }
        tempFile?.delete()
        tempFile = null
    }

    @Suppress("DEPRECATION")
    private fun createRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()

    private fun moveToStorage(temp: File): File {
        val target = File(context.filesDir, DIR_NAME).apply { mkdirs() }
            .let { dir -> File(dir, "voice_${System.currentTimeMillis()}.m4a") }
        return if (temp.renameTo(target)) {
            target
        } else {
            runCatching { temp.copyTo(target, overwrite = true) }
                .onSuccess { temp.delete() }
                .getOrElse { temp }
        }
    }

    companion object {
        const val DIR_NAME = "voice"
        private const val TAG = "Voice"
        private const val BIT_RATE = 96_000
        private const val SAMPLE_RATE = 44_100
    }
}