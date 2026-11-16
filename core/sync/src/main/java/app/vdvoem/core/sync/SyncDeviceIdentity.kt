package app.vdvoem.core.sync

import app.vdvoem.core.domain.device.DeviceIdentity

/** Id установки живёт в настройках синка, поэтому реализация именно здесь. */
class SyncDeviceIdentity(
    private val settingsStore: SyncSettingsStore
) : DeviceIdentity {
    override suspend fun deviceId(): String = settingsStore.deviceId()
}