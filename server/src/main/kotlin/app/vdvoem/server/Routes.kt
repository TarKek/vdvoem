package app.vdvoem.server

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.call
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

private const val TOKEN_PREFIX = "Bearer "

fun Application.configureRouting(repository: Repository, mediaRepository: MediaRepository) {
    routing {
        get("/health") {
            call.respond(HealthResponse(status = "ok", time = System.currentTimeMillis()))
        }

        route("/v1") {
            attachmentRoutes(repository, mediaRepository)
            pluginRoutes(mediaRepository)

            post("/pairs") {
                val request = call.receive<CreatePairRequest>()
                if (request.nameA.isBlank() || request.nameB.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("names are required"))
                    return@post
                }
                val (pairId, token, inviteCode) = withContext(Dispatchers.IO) {
                    repository.createPair(request)
                }
                call.respond(AuthResponse(pairId, token, inviteCode))
            }

            post("/pairs/join") {
                val request = call.receive<JoinPairRequest>()
                val result = withContext(Dispatchers.IO) { repository.join(request.inviteCode) }
                if (result == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("invite code not found"))
                } else {
                    call.respond(AuthResponse(result.first, result.second, request.inviteCode.uppercase()))
                }
            }

            post("/pairs/leave") {
                val token = call.bearerToken()
                val revoked = token != null && withContext(Dispatchers.IO) {
                    repository.revokeDevice(token)
                }
                if (!revoked) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("unauthorized"))
                    return@post
                }
                PushHub.revokeToken(Db.hash(token!!))
                call.respond(HttpStatusCode.NoContent)
            }

            get("/sync") {
                val token = call.bearerToken()
                val device = withContext(Dispatchers.IO) {
                    token?.let { repository.authenticateSyncDevice(it, call.request.headers["X-Device-Id"].orEmpty()) }
                }
                if (device == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("unauthorized"))
                    return@get
                }
                val pairId = device.pairId
                val since = call.request.queryParameters["since"]?.toLongOrNull() ?: 0L
                val (cursor, changes) = withContext(Dispatchers.IO) {
                    val cursor = repository.currentCursor(pairId)
                    // Старые клиенты присылают epoch-millis: один раз делаем полный pull.
                    val effectiveSince = since.coerceAtLeast(0L).takeIf { it <= cursor } ?: 0L
                    cursor to repository.pull(pairId, effectiveSince, device.deviceId)
                }
                if (call.isLegacySyncClient()) {
                    call.respond(legacySyncResponse(cursor, changes))
                } else {
                    call.respond(SyncResponse(cursor, changes))
                }
            }

            post("/sync") {
                val token = call.bearerToken()
                val device = withContext(Dispatchers.IO) {
                    token?.let { repository.authenticateSyncDevice(it, call.request.headers["X-Device-Id"].orEmpty()) }
                }
                if (device == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("unauthorized"))
                    return@post
                }
                val pairId = device.pairId
                val raw = call.receiveText()
                if (raw.length > MAX_SYNC_BODY_CHARS) {
                    call.respond(HttpStatusCode.PayloadTooLarge, ErrorResponse("sync body too large"))
                    return@post
                }
                val request = parsePushRequest(raw)
                if (request == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid json"))
                    return@post
                }
                val validationError = validateSyncRequest(pairId, device.deviceId, request.changes)
                if (validationError != null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(validationError))
                    return@post
                }
                val (cursor, changes) = withContext(Dispatchers.IO) {
                    repository.push(pairId, device.deviceId, request.changes)
                    val cursor = repository.currentCursor(pairId)
                    val effectiveSince = request.baseTime.coerceAtLeast(0L).takeIf { it <= cursor } ?: 0L
                    cursor to repository.pull(pairId, effectiveSince, device.deviceId)
                }
                PushHub.notifyPair(pairId, exceptTokenHash = token?.let(Db::hash), serverTime = cursor)
                if (call.isLegacySyncClient()) {
                    call.respond(legacySyncResponse(cursor, changes))
                } else {
                    call.respond(SyncResponse(cursor, changes))
                }
            }
        }

        adminRoutes(mediaRepository)
    }
}

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
private fun Application.installJson() {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        })
    }
}

private val syncJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** Старые APK без X-Device-Id продолжают получать плоский ChangesDto + serverTime. */
private fun io.ktor.server.application.ApplicationCall.isLegacySyncClient(): Boolean =
    request.headers["X-Device-Id"].isNullOrBlank()

private fun legacySyncResponse(serverTime: Long, changes: ChangesDto): JsonObject =
    JsonObject(
        syncJson.encodeToJsonElement(ChangesDto.serializer(), changes).jsonObject +
            ("serverTime" to JsonPrimitive(serverTime))
    )

private const val MAX_SYNC_BODY_CHARS = 2_000_000
private const val MAX_SYNC_CHANGES = 5_000
private const val MAX_CLOCK_SKEW_MILLIS = 24L * 60 * 60 * 1000

/**
 * Разбирает тело `POST /v1/sync`; `null` — тело битое или неполное (клиенту нужен 400, а не 500).
 */
internal fun parsePushRequest(raw: String): PushRequest? =
    runCatching { syncJson.decodeFromString<PushRequest>(raw) }.getOrNull()

internal fun validateSyncRequest(pairId: String, deviceId: String, changes: ChangesDto): String? {
    val ids = buildList {
        changes.pair?.let { add(it.id) }
        changes.events.forEach { add(it.id) }
        changes.wishes.forEach { add(it.id) }
        changes.gifts.forEach { add(it.id) }
        changes.pokes.forEach { add(it.id) }
        changes.quizScores.forEach { add(it.id) }
        changes.lessons.forEach { add(it.id) }
        changes.tasks.forEach { add(it.id) }
        changes.cycleDays.forEach { add(it.id) }
        changes.cycleSettings.forEach { add(it.id) }
        changes.diaryEntries.forEach { add(it.id) }
        changes.diaryReminders.forEach { add(it.id) }
    }
    if (ids.size > MAX_SYNC_CHANGES) return "too many changes"
    if (ids.any { runCatching { UUID.fromString(it) }.isFailure }) return "invalid entity id"
    if (changes.pair != null && changes.pair.id != pairId) return "pair id mismatch"
    if (deviceId.isBlank() && (changes.wishes.any { it.surprise } || changes.gifts.any { it.surprise })) {
        return "device identity required for surprises"
    }

    val timestamps = buildList {
        changes.pair?.let { add(it.updatedAt) }
        changes.events.forEach { add(it.updatedAt) }
        changes.wishes.forEach { add(it.updatedAt) }
        changes.gifts.forEach { add(it.updatedAt) }
        changes.pokes.forEach { add(it.updatedAt) }
        changes.quizScores.forEach { add(it.updatedAt) }
        changes.lessons.forEach { add(it.updatedAt) }
        changes.tasks.forEach { add(it.updatedAt) }
        changes.cycleDays.forEach { add(it.updatedAt) }
        changes.cycleSettings.forEach { add(it.updatedAt) }
        changes.diaryEntries.forEach { add(it.updatedAt) }
        changes.diaryReminders.forEach { add(it.updatedAt) }
    }
    val newestAllowed = System.currentTimeMillis() + MAX_CLOCK_SKEW_MILLIS
    if (timestamps.any { it < 0L || it > newestAllowed }) return "invalid updatedAt"
    return null
}

fun io.ktor.server.application.ApplicationCall.bearerToken(): String? {
    val header = request.headers["Authorization"] ?: return null
    return header.removePrefix(TOKEN_PREFIX).takeIf { it.isNotBlank() }
}

/** Возвращает pair_id из токена; при отсутствии/невалидности отвечает 401 и возвращает null. */
suspend fun io.ktor.server.application.ApplicationCall.requirePair(
    repository: Repository
): String? {
    val pairId = bearerToken()?.let(repository::authenticate)
    if (pairId == null) {
        respond(HttpStatusCode.Unauthorized, ErrorResponse("unauthorized"))
    }
    return pairId
}

fun Application.configureSerialization() = installJson()