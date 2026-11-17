package app.vdvoem.feature.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vdvoem.feature.voice.audio.VoiceNotePlayer
import app.vdvoem.feature.voice.audio.VoiceNoteRecorder
import app.vdvoem.feature.voice.data.VoiceNote
import app.vdvoem.feature.voice.data.VoiceNoteStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class VoiceUiState(
    val notes: List<VoiceNote> = emptyList(),
    val isRecording: Boolean = false,
    /** Секунды с начала текущей записи; тикают раз в секунду. */
    val elapsedSeconds: Int = 0,
    val playingId: String? = null,
    /** Заметка, ожидающая подтверждения удаления. */
    val pendingDelete: VoiceNote? = null,
    /** Строковый ресурс с сообщением об ошибке. */
    val errorRes: Int? = null
) {
    val isEmpty: Boolean
        get() = notes.isEmpty()
}

class VoiceViewModel(
    private val store: VoiceNoteStore,
    private val recorder: VoiceNoteRecorder,
    private val player: VoiceNotePlayer
) : ViewModel() {

    private val _state = MutableStateFlow(VoiceUiState())
    val state: StateFlow<VoiceUiState> = _state.asStateFlow()

    private var ticker: Job? = null

    init {
        viewModelScope.launch {
            store.notes.collect { notes ->
                _state.value = _state.value.copy(notes = notes)
            }
        }
    }

    /** Стартует запись. Разрешение запрашивает экран до вызова. */
    fun startRecording() {
        if (_state.value.isRecording) return
        player.stop()
        _state.value = _state.value.copy(playingId = null)
        if (!recorder.start()) {
            _state.value = _state.value.copy(errorRes = R.string.voice_error_record)
            return
        }
        _state.value = _state.value.copy(
            isRecording = true,
            elapsedSeconds = 0,
            playingId = null,
            errorRes = null
        )
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive && _state.value.isRecording) {
                delay(1_000L)
                _state.value = _state.value.copy(
                    elapsedSeconds = _state.value.elapsedSeconds + 1
                )
            }
        }
    }

    /** Останавливает запись и сохраняет заметку. */
    fun stopRecording() {
        if (!_state.value.isRecording) return
        ticker?.cancel()
        ticker = null
        val recording = recorder.stop()
        val elapsed = _state.value.elapsedSeconds
        _state.value = _state.value.copy(isRecording = false, elapsedSeconds = 0)
        if (recording == null) {
            _state.value = _state.value.copy(errorRes = R.string.voice_error_record)
            return
        }
        val note = VoiceNote(
            id = UUID.randomUUID().toString(),
            title = "",
            filePath = recording.file.absolutePath,
            durationMs = if (recording.durationMs > 0L) recording.durationMs else elapsed * 1_000L,
            createdAt = System.currentTimeMillis()
        )
        viewModelScope.launch { store.save(note) }
    }

    /** Проигрывает заметку или останавливает текущее проигрывание. */
    fun togglePlay(note: VoiceNote) {
        if (_state.value.isRecording) return
        if (_state.value.playingId == note.id) {
            player.stop()
            _state.value = _state.value.copy(playingId = null)
            return
        }
        val started = player.play(File(note.filePath)) {
            _state.value = _state.value.copy(playingId = null)
        }
        _state.value = _state.value.copy(
            playingId = if (started) note.id else null,
            errorRes = if (started) null else R.string.voice_error_play
        )
    }

    fun requestDelete(note: VoiceNote) {
        _state.value = _state.value.copy(pendingDelete = note)
    }

    fun cancelDelete() {
        _state.value = _state.value.copy(pendingDelete = null)
    }

    fun confirmDelete() {
        val note = _state.value.pendingDelete ?: return
        if (_state.value.playingId == note.id) {
            player.stop()
            _state.value = _state.value.copy(playingId = null)
        }
        _state.value = _state.value.copy(pendingDelete = null)
        viewModelScope.launch { store.delete(note) }
    }

    fun dismissError() {
        _state.value = _state.value.copy(errorRes = null)
    }

    override fun onCleared() {
        ticker?.cancel()
        if (recorder.isRecording) recorder.cancel()
        player.release()
        super.onCleared()
    }
}