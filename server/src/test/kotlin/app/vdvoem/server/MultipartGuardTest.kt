package app.vdvoem.server

import io.ktor.http.ContentType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Регрессия: `POST /v1/attachments` с JSON-телом раньше падал в `receiveMultipart()`
 * с `IOException` и отвечал 500. Теперь такой запрос отсекается до чтения тела.
 */
class MultipartGuardTest {
    @Test
    fun `json body is rejected before reading multipart`() {
        assertFalse(isMultipart(ContentType.Application.Json))
        assertFalse(isMultipart(ContentType.Application.OctetStream))
        assertFalse(isMultipart(ContentType.Text.Plain))
    }

    @Test
    fun `missing content type is rejected`() {
        assertFalse(isMultipart(null))
    }

    @Test
    fun `multipart form data with boundary is accepted`() {
        val type = ContentType.MultiPart.FormData.withParameter("boundary", "----vdvoem")

        assertTrue(isMultipart(type))
    }
}