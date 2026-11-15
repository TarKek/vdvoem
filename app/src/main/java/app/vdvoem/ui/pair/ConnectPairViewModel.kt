package app.vdvoem.ui.pair

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vdvoem.core.domain.model.Pair
import app.vdvoem.core.domain.repository.PairRepository
import app.vdvoem.core.domain.time.Dates
import app.vdvoem.core.sync.SyncController
import app.vdvoem.core.sync.SyncResult
import app.vdvoem.core.sync.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConnectPairViewModel(
    private val controller: SyncController,
    private val pairRepository: PairRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ConnectState>(ConnectState.Form())
    val state: StateFlow<ConnectState> = _state.asStateFlow()

    init {
        // Уже настроенная локально пара — подсказка для полей создания.
        viewModelScope.launch {
            val local = runCatching { pairRepository.current() }.getOrNull() ?: return@launch
            _state.update { current ->
                if (current !is ConnectState.Form) return@update current
                current.copy(
                    nameA = local.nameA.takeUnless { it == DEFAULT_NAME_A }.orEmpty(),
                    nameB = local.nameB.takeUnless { it == DEFAULT_NAME_B }.orEmpty(),
                    sinceMillis = local.sinceMillis.takeIf { it > 0 } ?: current.sinceMillis
                )
            }
        }
        // Пара уже подключена — сразу показываем её, а не форму создания.
        viewModelScope.launch {
            combine(controller.settings, pairRepository.observe(), controller.status) { settings, pair, status ->
                Triple(settings, pair, status)
            }.collect { (settings, pair, status) ->
                val current = _state.value
                if (current is ConnectState.Created) return@collect
                if (!settings.isConfigured) {
                    if (current is ConnectState.Connected) {
                        _state.value = ConnectState.Form(serverUrl = current.serverUrl)
                    }
                    return@collect
                }
                val base = if (current is ConnectState.Connected) current else null
                _state.value = ConnectState.Connected(
                    serverUrl = settings.serverUrl,
                    pairId = settings.pairId,
                    inviteCode = settings.inviteCode.ifBlank { base?.inviteCode.orEmpty() },
                    nameA = pair?.nameA?.takeUnless { it == DEFAULT_NAME_A }.orEmpty(),
                    nameB = pair?.nameB?.takeUnless { it == DEFAULT_NAME_B }.orEmpty(),
                    sinceMillis = pair?.sinceMillis ?: 0L,
                    syncing = status is SyncStatus.Running,
                    lastSyncMillis = base?.lastSyncMillis ?: 0L,
                    error = (status as? SyncStatus.Error)?.message
                )
            }
        }
    }

    fun onServerChange(value: String) {
        _state.update { current ->
            if (current is ConnectState.Form) current.copy(serverUrl = value, error = null) else current
        }
    }

    fun onInviteCodeChange(value: String) {
        _state.update { current ->
            if (current is ConnectState.Form) {
                current.copy(inviteCode = ConnectState.normalizeInviteCode(value), error = null)
            } else {
                current
            }
        }
    }

    fun onNamesChange(nameA: String, nameB: String) {
        _state.update { current ->
            if (current is ConnectState.Form) current.copy(nameA = nameA, nameB = nameB, error = null) else current
        }
    }

    fun onSinceChange(value: Long) {
        _state.update { current ->
            if (current is ConnectState.Form) current.copy(sinceMillis = value, error = null) else current
        }
    }

    fun createPair() {
        val form = _state.value as? ConnectState.Form ?: return
        if (form.serverUrl.isBlank()) {
            _state.value = form.copy(error = "Укажите адрес сервера")
            return
        }
        _state.value = form.copy(busy = true, error = null)
        viewModelScope.launch {
            controller.setServerUrl(form.serverUrl)
            val localPair = runCatching { pairRepository.current() }.getOrNull()
            val res = controller.createPair(
                nameA = form.nameA.trim().ifBlank { localPair?.nameA ?: DEFAULT_NAME_A },
                nameB = form.nameB.trim().ifBlank { localPair?.nameB ?: DEFAULT_NAME_B },
                sinceMillis = validSince(form, localPair),
                birthdayA = localPair?.birthdayA,
                birthdayB = localPair?.birthdayB
            )
            when (val result = res) {
                is SyncResult.Success -> {
                    val settings = controller.currentSettings()
                    _state.value = ConnectState.Created(
                        pairId = settings.pairId,
                        inviteCode = settings.inviteCode
                    )
                }
                is SyncResult.Error -> _state.value =
                    form.copy(busy = false, error = result.message)
                SyncResult.NotConfigured -> _state.value =
                    form.copy(busy = false, error = "Проверьте адрес сервера")
            }
        }
    }

    fun join(code: String? = null) {
        val form = _state.value as? ConnectState.Form ?: return
        val inviteCode = ConnectState.normalizeInviteCode(code ?: form.inviteCode)
        if (form.serverUrl.isBlank()) {
            _state.value = form.copy(inviteCode = inviteCode, error = "Укажите адрес сервера")
            return
        }
        if (inviteCode.length < ConnectState.INVITE_CODE_LENGTH) return
        _state.value = form.copy(inviteCode = inviteCode, busy = true, error = null)
        viewModelScope.launch {
            controller.setServerUrl(form.serverUrl)
            when (val result = controller.joinPair(inviteCode)) {
                is SyncResult.Success -> {
                    val settings = controller.currentSettings()
                    _state.value = ConnectState.Created(
                        pairId = settings.pairId,
                        inviteCode = form.inviteCode
                    )
                }
                is SyncResult.Error -> _state.value =
                    form.copy(busy = false, error = result.message)
                SyncResult.NotConfigured -> _state.value =
                    form.copy(busy = false, error = "Проверьте адрес сервера")
            }
        }
    }

    /** Ручной проход: тот самый «потянуть, чтобы обновить» с этого экрана. */
    fun syncNow() {
        val current = _state.value as? ConnectState.Connected ?: return
        _state.value = current.copy(syncing = true, error = null)
        viewModelScope.launch {
            val result = controller.syncNow()
            val latest = _state.value as? ConnectState.Connected ?: return@launch
            _state.value = when (result) {
                is SyncResult.Success ->
                    latest.copy(syncing = false, error = null, lastSyncMillis = System.currentTimeMillis())
                is SyncResult.Error -> latest.copy(syncing = false, error = result.message)
                SyncResult.NotConfigured -> latest.copy(syncing = false, error = "Синхронизация не настроена")
            }
        }
    }

    fun leavePair() {
        viewModelScope.launch {
            val connected = _state.value as? ConnectState.Connected
            val knownUrl = connected?.serverUrl
                ?.takeIf { it.isNotBlank() }
                ?: runCatching { controller.currentSettings().serverUrl }.getOrNull().orEmpty()
            val knownSince = connected?.sinceMillis?.takeIf { it > 0 }
            controller.leavePair()
            val local = runCatching { pairRepository.current() }.getOrNull()
            _state.value = ConnectState.Form(
                serverUrl = knownUrl,
                nameA = connected?.nameA?.takeIf { it.isNotBlank() } ?: local?.nameA.orEmpty(),
                nameB = connected?.nameB?.takeIf { it.isNotBlank() } ?: local?.nameB.orEmpty(),
                sinceMillis = knownSince
                    ?: local?.sinceMillis?.takeIf { it > 0 }
                    ?: Dates.startOfDay(Dates.today())
            )
        }
    }

    /** Экран «Пара создана» → карточка подключённой пары. */
    fun dismissCreated() {
        val current = _state.value as? ConnectState.Created ?: return
        viewModelScope.launch {
            val settings = controller.currentSettings()
            _state.value = ConnectState.Connected(
                serverUrl = settings.serverUrl,
                pairId = current.pairId,
                inviteCode = current.inviteCode
            )
        }
    }

    /**
     * Дата начала никогда не должна остаться нулём: сервер отдал бы её как
     * «1 января 1970» и отсчёт в 20 000 дней. Если своей даты нет — берём сегодня.
     */
    private fun validSince(form: ConnectState.Form, local: Pair?): Long = when {
        form.sinceMillis > 0 -> form.sinceMillis
        (local?.sinceMillis ?: 0L) > 0 -> local!!.sinceMillis
        else -> Dates.startOfDay(Dates.today())
    }

    private companion object {
        const val DEFAULT_NAME_A = "Я"
        const val DEFAULT_NAME_B = "Партнёр"
    }
}