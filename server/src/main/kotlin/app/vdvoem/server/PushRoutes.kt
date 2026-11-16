package app.vdvoem.server

import io.ktor.server.application.log
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.launch

/**
 * `GET /v1/ws` — постоянный канал «что-то изменилось» (docs/CYCLE_SPEC.md §8).
 *
 * Аутентификация та же, что у HTTP-маршрутов: `Authorization: Bearer <deviceToken>`,
 * поиск пары — через [Repository.authenticate] (тот же SHA-256 и та же таблица `devices`).
 * Невалидный или отсутствующий токен закрывает сокет с кодом 1008, апгрейд не остаётся
 * «висящим» подключением без пары.
 *
 * Протокол односторонний и намеренно бедный: сервер присылает
 * `{"type":"changed","serverTime":<millis>}`, клиент ничего не шлёт. Пинг-понг и
 * закрытие соединения обслуживает [io.ktor.server.websocket.WebSockets] plugin.
 */
fun io.ktor.server.application.Application.pushRoutes(repository: Repository) {
    routing {
        webSocket("/v1/ws") {
            val token = call.bearerToken()
            val pairId = try {
                token?.let(repository::authenticate)
            } catch (error: Throwable) {
                call.application.log.warn("push auth failed", error)
                null
            }
            if (pairId == null || token == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "unauthorized"))
                return@webSocket
            }

            val connection = PushHub.register(pairId, Db.hash(token))
            val sender = launch {
                // Единственный писатель в сокет: кадры не перемешиваются между запросами.
                for (payload in connection.outgoing) {
                    send(Frame.Text(payload))
                }
            }
            try {
                for (frame in incoming) {
                    // Сообщения от клиента не нужны; текстовые читаем, чтобы освободить буфер.
                    if (frame is Frame.Text) frame.readText()
                }
            } finally {
                sender.cancel()
                PushHub.unregister(connection)
            }
        }
    }
}