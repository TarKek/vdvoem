package app.vdvoem.core.sync

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

/** Ошибка обращения к серверу синхронизации. */
class SyncException(
    message: String,
    val statusCode: Int? = null,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * HTTP-клиент контракта v1 (docs/SYNC_API.md).
 *
 * Токен и базовый URL берутся из [SyncSettingsStore] на каждый запрос — смена сервера
 * или повторное вступление в пару не требуют пересоздания клиента.
 */
class SyncApi(private val settings: SyncSettingsStore) {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        coerceInputValues = true
    }

    private val client = HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = TIMEOUT_MILLIS
            connectTimeoutMillis = TIMEOUT_MILLIS
            socketTimeoutMillis = TIMEOUT_MILLIS
        }
    }

    /** `POST /v1/pairs` — создать пару и получить токен устройства. */
    suspend fun createPair(
        nameA: String = "",
        nameB: String = "",
        sinceMillis: Long = 0L,
        birthdayA: Long? = null,
        birthdayB: Long? = null
    ): PairCredentialsDto = wrap {
        val cfg = configUrl()
        client.post("${cfg.baseUrl}/v1/pairs") {
            contentType(ContentType.Application.Json)
            setBody(
                CreatePairRequestDto(
                    nameA = nameA,
                    nameB = nameB,
                    sinceMillis = sinceMillis,
                    birthdayA = birthdayA,
                    birthdayB = birthdayB
                )
            )
        }.body()
    }

    /** `POST /v1/pairs/join` — вступить в существующую пару по коду. */
    suspend fun joinPair(code: String): PairCredentialsDto = wrap {
        val cfg = configUrl()
        client.post("${cfg.baseUrl}/v1/pairs/join") {
            contentType(ContentType.Application.Json)
            setBody(JoinRequestDto(inviteCode = code.trim()))
        }.body()
    }

    /** Отзывает текущий токен; локальные credentials очищаются контроллером даже офлайн. */
    suspend fun leavePair(snapshot: SyncSettings): Unit = wrap {
        client.post("${snapshot.baseUrl}/v1/pairs/leave") {
            bearerAuth(snapshot.deviceToken)
        }
    }

    /** `GET /v1/sync?since=<cursor>` — изменения после непрозрачного серверного курсора. */
    suspend fun pull(since: Long, snapshot: SyncSettings? = null): ChangesDto = wrap {
        val cfg = snapshot ?: config()
        val text = client.get("${cfg.baseUrl}/v1/sync") {
            bearerAuth(cfg.deviceToken)
            header("X-Device-Id", settings.deviceId())
            parameter("since", since)
        }.bodyAsText()
        decode(text)
    }

    /** `POST /v1/sync` — отправить локальные изменения; сервер применяет LWW. */
    suspend fun push(changes: ChangesDto, snapshot: SyncSettings? = null): ChangesDto = wrap {
        val cfg = snapshot ?: config()
        val text = client.post("${cfg.baseUrl}/v1/sync") {
            bearerAuth(cfg.deviceToken)
            header("X-Device-Id", settings.deviceId())
            contentType(ContentType.Application.Json)
            setBody(PushRequestDto(baseTime = cfg.lastServerTime, changes = changes))
        }.bodyAsText()
        decode(text)
    }

    private fun decode(text: String): ChangesDto {
        if (text.isBlank()) return ChangesDto()
        // Сервер отвечает обёрткой {serverTime, changes}. Держим совместимость с плоским форматом.
        val root = json.parseToJsonElement(text).jsonObject
        val changes = root["changes"]?.jsonObject
        return if (changes == null) {
            json.decodeFromJsonElement(ChangesDto.serializer(), root)
        } else {
            json.decodeFromJsonElement(ChangesDto.serializer(), changes).copy(
                serverTime = (root["serverTime"] as? JsonPrimitive)?.longOrNull ?: 0L
            )
        }
    }

    private suspend fun configUrl(): SyncSettings {
        val cfg = settings.current()
        if (cfg.baseUrl.isBlank()) {
            throw SyncException("Укажите адрес сервера")
        }
        return cfg
    }

    private suspend fun config(): SyncSettings {
        val cfg = settings.current()
        if (!cfg.isConfigured) {
            throw SyncException("Синхронизация не настроена: укажите адрес сервера и токен устройства")
        }
        return cfg
    }

    private suspend fun <T> wrap(block: suspend () -> T): T = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: SyncException) {
        throw error
    } catch (error: ResponseException) {
        val code = error.response.status.value
        throw SyncException(
            message = when (code) {
                401 -> "Нет доступа: токен устройства отсутствует или отозван"
                409 -> "Конфликт версий: повторите синхронизацию"
                413 -> "Слишком большой объём изменений"
                else -> "Сервер вернул ошибку $code"
            },
            statusCode = code,
            cause = error
        )
    } catch (error: Exception) {
        throw SyncException(
            message = error.message ?: "Не удалось связаться с сервером (${error::class.simpleName})",
            cause = error
        )
    }

    private companion object {
        const val TIMEOUT_MILLIS = 20_000L
    }
}