package app.vdvoem.core.sync

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Локальные настройки синхронизации.
 *
 * [lastServerTime] — метка `serverTime` последнего успешного ответа сервера. Именно она
 * уходит в `since`, часы телефона не используются (см. docs/SYNC_API.md).
 */
data class SyncSettings(
    val serverUrl: String = "",
    val deviceToken: String = "",
    val pairId: String = "",
    val inviteCode: String = "",
    val lastServerTime: Long = 0L,
    /** Наибольший локальный `updatedAt`, который уже успешно уехал на сервер. */
    val lastPushedAt: Long = 0L,
    val enabled: Boolean = false
) {
    /** Есть сервер и токен — можно ходить в сеть. */
    val isConfigured: Boolean
        get() = serverUrl.isNotBlank() && deviceToken.isNotBlank()

    /** Базовый URL без слэша в конце, как его ждёт [SyncApi]. */
    val baseUrl: String
        get() = serverUrl.trim().trimEnd('/')
}

private val Context.syncDataStore by preferencesDataStore(name = "sync_prefs")

/** DataStore-хранилище настроек синка (файл `sync_prefs`). */
class SyncSettingsStore(private val context: Context) {

    val settings: Flow<SyncSettings> = context.syncDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            SyncSettings(
                serverUrl = prefs[KEY_SERVER_URL].orEmpty(),
                deviceToken = prefs[KEY_DEVICE_TOKEN].orEmpty(),
                pairId = prefs[KEY_PAIR_ID].orEmpty(),
                inviteCode = prefs[KEY_INVITE_CODE].orEmpty(),
                lastServerTime = prefs[KEY_LAST_SERVER_TIME] ?: 0L,
                lastPushedAt = prefs[KEY_LAST_PUSHED_AT] ?: 0L,
                enabled = prefs[KEY_ENABLED] ?: false
            )
        }

    /** Текущие настройки одним значением (для suspend-кода воркера и движка). */
    suspend fun current(): SyncSettings = settings.first()

    suspend fun setServerUrl(url: String) {
        context.syncDataStore.edit { it[KEY_SERVER_URL] = url.trim().trimEnd('/') }
    }

    suspend fun setDeviceToken(token: String) {
        context.syncDataStore.edit { it[KEY_DEVICE_TOKEN] = token }
    }

    suspend fun setPairId(pairId: String) {
        context.syncDataStore.edit { it[KEY_PAIR_ID] = pairId }
    }

    suspend fun setInviteCode(code: String) {
        context.syncDataStore.edit { it[KEY_INVITE_CODE] = code }
    }

    suspend fun setLastServerTime(time: Long) {
        context.syncDataStore.edit { it[KEY_LAST_SERVER_TIME] = time }
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.syncDataStore.edit { it[KEY_ENABLED] = enabled }
    }

    suspend fun setLastPushedAt(time: Long) {
        context.syncDataStore.edit { it[KEY_LAST_PUSHED_AT] = time }
    }

    /** Сохранить всё, что вернул сервер при создании/вступлении в пару. */
    suspend fun saveCredentials(pairId: String, deviceToken: String, inviteCode: String) {
        context.syncDataStore.edit { prefs ->
            prefs[KEY_PAIR_ID] = pairId
            prefs[KEY_DEVICE_TOKEN] = deviceToken
            prefs[KEY_INVITE_CODE] = inviteCode
            prefs[KEY_ENABLED] = true
        }
    }

    /**
     * Выход из пары: забываем устройство и пару, но адрес сервера оставляем —
     * иначе после «Выйти из пары» поле адреса пустое и войти по коду нельзя.
     */
    suspend fun clearCredentials() {
        context.syncDataStore.edit { prefs ->
            prefs.remove(KEY_DEVICE_TOKEN)
            prefs.remove(KEY_PAIR_ID)
            prefs.remove(KEY_INVITE_CODE)
            prefs.remove(KEY_LAST_SERVER_TIME)
            prefs.remove(KEY_LAST_PUSHED_AT)
            prefs[KEY_ENABLED] = false
        }
    }

    /** Полный сброс: выход из пары, смена сервера, «забыть устройство». */
    suspend fun clear() {
        context.syncDataStore.edit { prefs -> prefs.clear() }
    }

    private companion object {
        val KEY_SERVER_URL = stringPreferencesKey("server_url")
        val KEY_DEVICE_TOKEN = stringPreferencesKey("device_token")
        val KEY_PAIR_ID = stringPreferencesKey("pair_id")
        val KEY_INVITE_CODE = stringPreferencesKey("invite_code")
        val KEY_LAST_SERVER_TIME = longPreferencesKey("last_server_time")
        val KEY_LAST_PUSHED_AT = longPreferencesKey("last_pushed_at")
        val KEY_ENABLED = booleanPreferencesKey("enabled")
    }
}