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
import java.util.UUID

/**
 * Локальные настройки синхронизации.
 *
 * [lastServerTime] — непрозрачный курсор последнего успешного ответа сервера.
 */
data class SyncSettings(
    val serverUrl: String = "",
    val deviceToken: String = "",
    val pairId: String = "",
    val inviteCode: String = "",
    val lastServerTime: Long = 0L,
    /** Наибольший локальный `updatedAt`, который уже успешно уехал на сервер. */
    val lastPushedAt: Long = 0L,
    val enabled: Boolean = false,
    /** Стабильный id установки: отличает свои «сюрпризы» от чужих. */
    val localDeviceId: String = ""
) {
    /** Есть сервер и токен — можно ходить в сеть. */
    val isConfigured: Boolean
        get() = serverUrl.isNotBlank() && deviceToken.isNotBlank()

    /** Базовый URL без слэша в конце, как его ждёт [SyncApi]. */
    val baseUrl: String
        get() = serverUrl.trim().trimEnd('/')
}

private fun migrateProductionUrl(url: String): String = when (url.trim().trimEnd('/')) {
    "http://185.213.240.43:18090" -> "https://185.213.240.43.sslip.io:18443"
    else -> url
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
                serverUrl = migrateProductionUrl(prefs[KEY_SERVER_URL].orEmpty()),
                deviceToken = prefs[KEY_DEVICE_TOKEN].orEmpty(),
                pairId = prefs[KEY_PAIR_ID].orEmpty(),
                inviteCode = prefs[KEY_INVITE_CODE].orEmpty(),
                lastServerTime = prefs[KEY_LAST_SERVER_TIME] ?: 0L,
                lastPushedAt = prefs[KEY_LAST_PUSHED_AT] ?: 0L,
                enabled = prefs[KEY_ENABLED] ?: false,
                localDeviceId = prefs[KEY_LOCAL_DEVICE_ID].orEmpty()
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

    /** Подтверждённые сервером версии записей; обратная сторона persistent outbox. */
    suspend fun acknowledgements(pairId: String): Map<String, Long> {
        val prefix = "$pairId/"
        return decodeAcknowledgements(context.syncDataStore.data.first()[KEY_ACKNOWLEDGED].orEmpty())
            .filterKeys { it.startsWith(prefix) }
    }

    /** Отмечает только фактически отправленные/полученные версии, не глобальный timestamp. */
    suspend fun markPushed(pairId: String, changes: ChangesDto) {
        val stamps = buildMap {
            changes.pair?.let { put(syncAckKey(pairId, "pair", it.id), it.updatedAt) }
            changes.events.forEach { put(syncAckKey(pairId, "event", it.id), it.updatedAt) }
            changes.wishes.forEach { put(syncAckKey(pairId, "wish", it.id), it.updatedAt) }
            changes.gifts.forEach { put(syncAckKey(pairId, "gift", it.id), it.updatedAt) }
            changes.pokes.forEach { put(syncAckKey(pairId, "poke", it.id), it.updatedAt) }
            changes.quizScores.forEach { put(syncAckKey(pairId, "quiz", it.id), it.updatedAt) }
            changes.lessons.forEach { put(syncAckKey(pairId, "lesson", it.id), it.updatedAt) }
            changes.tasks.forEach { put(syncAckKey(pairId, "task", it.id), it.updatedAt) }
            changes.cycleDays.forEach { put(syncAckKey(pairId, "cycleDay", it.id), it.updatedAt) }
            changes.cycleSettings.forEach { put(syncAckKey(pairId, "cycleSettings", it.id), it.updatedAt) }
            changes.diaryEntries.forEach { put(syncAckKey(pairId, "diary", it.id), it.updatedAt) }
            changes.diaryReminders.forEach {
                put(syncAckKey(pairId, "diaryReminder", it.id), it.updatedAt)
            }
        }
        if (stamps.isEmpty()) return
        context.syncDataStore.edit { prefs ->
            val current = decodeAcknowledgements(prefs[KEY_ACKNOWLEDGED].orEmpty())
            prefs[KEY_ACKNOWLEDGED] = encodeAcknowledgements(current + stamps)
        }
    }

    private fun decodeAcknowledgements(raw: String): Map<String, Long> = buildMap {
        raw.lineSequence().forEach { line ->
            val separator = line.lastIndexOf('\t')
            if (separator <= 0) return@forEach
            val timestamp = line.substring(separator + 1).toLongOrNull() ?: return@forEach
            put(line.substring(0, separator), timestamp)
        }
    }

    private fun encodeAcknowledgements(stamps: Map<String, Long>): String =
        stamps.entries.joinToString("\n") { "${it.key}\t${it.value}" }

    /**
     * Id установки: генерируется один раз при первом обращении и дальше не меняется.
     * Нужен, чтобы «сюрприз» автора оставался видимым только на его устройстве.
     */
    suspend fun deviceId(): String {
        val saved = current().localDeviceId
        if (saved.isNotBlank()) return saved
        val generated = UUID.randomUUID().toString()
        context.syncDataStore.edit { prefs ->
            if (prefs[KEY_LOCAL_DEVICE_ID].isNullOrBlank()) prefs[KEY_LOCAL_DEVICE_ID] = generated
        }
        return current().localDeviceId.ifBlank { generated }
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
        val KEY_ACKNOWLEDGED = stringPreferencesKey("acknowledged_sync_versions")
        val KEY_ENABLED = booleanPreferencesKey("enabled")
        val KEY_LOCAL_DEVICE_ID = stringPreferencesKey("local_device_id")
    }
}

internal fun syncAckKey(pairId: String, type: String, id: String): String = "$pairId/$type/$id"