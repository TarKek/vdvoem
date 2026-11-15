package app.vdvoem.server

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.call
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

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
                val (pairId, token, inviteCode) = repository.createPair(request)
                call.respond(AuthResponse(pairId, token, inviteCode))
            }

            post("/pairs/join") {
                val request = call.receive<JoinPairRequest>()
                val result = repository.join(request.inviteCode)
                if (result == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("invite code not found"))
                } else {
                    call.respond(AuthResponse(result.first, result.second, request.inviteCode.uppercase()))
                }
            }

            get("/sync") {
                val token = call.bearerToken()
                val pairId = token?.let(repository::authenticate)
                if (pairId == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("unauthorized"))
                    return@get
                }
                val since = call.request.queryParameters["since"]?.toLongOrNull() ?: 0L
                call.respond(SyncResponse(System.currentTimeMillis(), repository.pull(pairId, since)))
            }

            post("/sync") {
                val token = call.bearerToken()
                val pairId = token?.let(repository::authenticate)
                if (pairId == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("unauthorized"))
                    return@post
                }
                val request = call.receive<PushRequest>()
                val now = System.currentTimeMillis()
                val applied = repository.push(pairId, request.changes)
                call.respond(PushResponse(now, applied))
            }
        }

        adminRoutes(mediaRepository)
    }
}

private fun Application.installJson() {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        })
    }
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