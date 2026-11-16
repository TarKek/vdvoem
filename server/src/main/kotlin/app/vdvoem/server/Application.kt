package app.vdvoem.server

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.websocket.WebSockets

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    Storage.ensure()
    val dataSource = Db.connect()
    val repository = Repository(dataSource)
    val mediaRepository = MediaRepository(dataSource)

    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        configureServer(repository, mediaRepository)
    }.start(wait = true)
}

fun Application.configureServer(repository: Repository, mediaRepository: MediaRepository) {
    install(CallLogging)
    configureSerialization()
    install(StatusPages) {
        exception<io.ktor.server.plugins.BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("bad request"))
        }
        exception<io.ktor.serialization.JsonConvertException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid json"))
        }
        exception<Throwable> { call, cause ->
            this@configureServer.log.error("request failed", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("internal error"))
        }
    }
    // Мгновенный пуш партнёру (docs/CYCLE_SPEC.md §8): `GET /v1/ws` + рассылка из `POST /v1/sync`.
    // Плагин ставим до маршрутов: билдер `webSocket` требует уже установленный WebSockets.
    install(WebSockets) {
        pingPeriodMillis = 20_000L
        timeoutMillis = 60_000L
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    configureRouting(repository, mediaRepository)
    pushRoutes(repository)
}