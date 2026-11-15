package app.vdvoem.ui.pair

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vdvoem.core.domain.repository.PairRepository
import app.vdvoem.core.sync.SyncController
import app.vdvoem.core.sync.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConnectPairViewModel(
    private val controller: SyncController,
    private val pairRepository: PairRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ConnectState>(ConnectState.Form())
    val state: StateFlow<ConnectState> = _state.asStateFlow()

    private val names = MutableStateFlow("" to "")

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
        names.value = nameA to nameB
    }

    fun createPair() {
        val form = _state.value as? ConnectState.Form ?: return
        if (form.serverUrl.isBlank()) return
        _state.value = form.copy(busy = true, error = null)
        viewModelScope.launch {
            controller.setServerUrl(form.serverUrl)
            val localPair = runCatching { pairRepository.current() }.getOrNull()
            val nameA = names.value.first.ifBlank { localPair?.nameA ?: "Я" }
            val nameB = names.value.second.ifBlank { localPair?.nameB ?: "Партнёр" }
            val res = controller.createPair(
                nameA = nameA,
                nameB = nameB,
                sinceMillis = localPair?.sinceMillis ?: 0L,
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
        if (form.serverUrl.isBlank() || inviteCode.length < ConnectState.INVITE_CODE_LENGTH) return
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
}