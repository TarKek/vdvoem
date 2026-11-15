package app.vdvoem.core.sync

import app.vdvoem.core.domain.model.Poke

/**
 * Что делать с сердечками, которые только что приехали с сервера.
 *
 * Ядро синка не умеет показывать уведомления (это платформенная часть), поэтому
 * реализация регистрируется в DI со стороны приложения.
 */
fun interface IncomingPokeHandler {
    suspend fun onIncoming(pokes: List<Poke>)
}