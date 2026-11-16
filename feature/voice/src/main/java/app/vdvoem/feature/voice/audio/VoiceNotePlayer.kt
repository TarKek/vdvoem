package app.vdvoem.feature.voice.audio

import android.media.MediaPlayer
import android.util.Log
import java.io.File

/**
 * Проигрывание голосовых заметок через [MediaPlayer].
 * Один экземпляр — одна активная запись; повторный [play] останавливает предыдущую.
 */
class VoiceNotePlayer {

    private var player: MediaPlayer? = null

    val isPlaying: Boolean
        get() = player?.isPlaying == true

    /** Начинает проигрывание; [onComplete] вызывается по завершении трека. */
    fun play(file: File, onComplete: () -> Unit): Boolean {
        stop()
        if (!file.exists()) return false
        val mediaPlayer = MediaPlayer()
        return try {
            mediaPlayer.setDataSource(file.absolutePath)
            mediaPlayer.setOnCompletionListener { finished ->
                runCatching { finished.release() }
                if (player === finished) player = null
                onComplete()
            }
            mediaPlayer.setOnErrorListener { failed, _, _ ->
                Log.w(TAG, "Ошибка проигрывания голосовой заметки")
                runCatching { failed.release() }
                if (player === failed) player = null
                onComplete()
                true
            }
            mediaPlayer.prepare()
            mediaPlayer.start()
            player = mediaPlayer
            true
        } catch (throwable: Throwable) {
            Log.w(TAG, "Не удалось проиграть голосовую заметку", throwable)
            runCatching { mediaPlayer.release() }
            false
        }
    }

    fun stop() {
        player?.let { active ->
            runCatching { active.stop() }
            runCatching { active.release() }
        }
        player = null
    }

    fun release() = stop()

    companion object {
        private const val TAG = "Voice"
    }
}