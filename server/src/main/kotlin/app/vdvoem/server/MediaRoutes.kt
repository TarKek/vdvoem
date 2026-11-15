package app.vdvoem.server

import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.streamProvider
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

private const val MAX_FILE_BYTES = 50L * 1024 * 1024
private const val MAX_APK_BYTES = 100L * 1024 * 1024
private const val ADMIN_HEADER = "X-Admin-Token"
private const val APK_MIME = "application/vnd.android.package-archive"

private val PLUGIN_ID = Regex("^[a-z0-9][a-z0-9._-]{0,63}$")
private val PLUGIN_VERSION = Regex("^[0-9A-Za-z][0-9A-Za-z._+-]{0,31}$")

// --- вложения -------------------------------------------------------------

fun Route.attachmentRoutes(repository: Repository, media: MediaRepository) {
    get("/attachments") {
        val pairId = call.requirePair(repository) ?: return@get
        val since = call.request.queryParameters["since"]?.toLongOrNull() ?: 0L
        call.respond(
            AttachmentsResponse(System.currentTimeMillis(), media.attachments(pairId, since))
        )
    }

    post("/attachments") {
        val pairId = call.requirePair(repository) ?: return@post
        val upload = try {
            call.receiveUpload("file", MAX_FILE_BYTES)
        } catch (_: TooLargeException) {
            call.respond(HttpStatusCode.PayloadTooLarge, ErrorResponse("file too large"))
            return@post
        }

        val file = upload.file
        if (file == null) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("file is required"))
            return@post
        }

        try {
            val entityType = upload.fields["entityType"]?.trim().orEmpty()
            val entityId = upload.fields["entityId"]?.trim().orEmpty()
            if (entityType.isEmpty() || entityType.length > 32 || uuidOrNull(entityId) == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("entityType and entityId are required"))
                return@post
            }
            if (file.size <= 0) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("file is empty"))
                return@post
            }

            val kind = upload.fields["kind"]?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: "file"
            val mime = upload.fileMime ?: upload.fields["mime"]?.trim()?.takeIf { it.isNotEmpty() }
                ?: "application/octet-stream"
            val storageKey = Storage.attachmentKey(file.sha256)
            Storage.commit(file, storageKey, replace = false)

            val saved = media.saveAttachment(
                pairId,
                NewAttachment(
                    entityType = entityType,
                    entityId = entityId,
                    kind = kind,
                    mime = mime,
                    size = file.size,
                    sha256 = file.sha256,
                    storageKey = storageKey,
                    durationMs = upload.fields["durationMs"]?.toLongOrNull(),
                    width = upload.fields["width"]?.toIntOrNull(),
                    height = upload.fields["height"]?.toIntOrNull()
                )
            )
            call.respond(HttpStatusCode.Created, saved)
        } finally {
            withContext(Dispatchers.IO) { Files.deleteIfExists(file.temp) }
        }
    }

    get("/attachments/{id}") {
        val pairId = call.requirePair(repository) ?: return@get
        val id = call.parameters["id"].orEmpty()
        val file = media.attachmentFile(pairId, id)
        val path = file?.let { Storage.pathOrNull(it.storageKey) }
        if (file == null || path == null || !Files.isRegularFile(path)) {
            call.respond(HttpStatusCode.NotFound, ErrorResponse("attachment not found"))
            return@get
        }

        val size = Files.size(path)
        val contentType = runCatching { ContentType.parse(file.mime) }
            .getOrDefault(ContentType.Application.OctetStream)
        call.response.header(HttpHeaders.AcceptRanges, "bytes")
        call.response.header(HttpHeaders.ETag, "\"${file.sha256}\"")
        call.response.header(HttpHeaders.CacheControl, "private, max-age=31536000, immutable")

        when (val range = parseRange(call.request.headers[HttpHeaders.Range], size)) {
            null -> call.respondOutputStream(contentType, HttpStatusCode.OK, size) {
                streamRange(path, 0, size)
            }
            ByteRange.Unsatisfiable -> {
                call.response.header(HttpHeaders.ContentRange, "bytes */$size")
                call.respond(
                    HttpStatusCode.RequestedRangeNotSatisfiable,
                    ErrorResponse("range not satisfiable")
                )
            }
            is ByteRange.Partial -> {
                val length = range.endInclusive - range.start + 1
                call.response.header(
                    HttpHeaders.ContentRange,
                    "bytes ${range.start}-${range.endInclusive}/$size"
                )
                call.respondOutputStream(contentType, HttpStatusCode.PartialContent, length) {
                    streamRange(path, range.start, length)
                }
            }
        }
    }

    delete("/attachments/{id}") {
        val pairId = call.requirePair(repository) ?: return@delete
        val id = call.parameters["id"].orEmpty()
        if (!media.deleteAttachment(pairId, id)) {
            call.respond(HttpStatusCode.NotFound, ErrorResponse("attachment not found"))
            return@delete
        }
        call.respond(HttpStatusCode.NoContent)
    }
}

// --- плагины --------------------------------------------------------------

fun Route.pluginRoutes(media: MediaRepository) {
    get("/plugins") {
        call.respond(media.plugins())
    }

    get("/plugins/{id}/apk") {
        val id = call.parameters["id"].orEmpty()
        val plugin = media.pluginFile(id)
        val path = plugin?.let { Storage.pathOrNull(it.storageKey) }
        if (plugin == null || path == null || !Files.isRegularFile(path)) {
            call.respond(HttpStatusCode.NotFound, ErrorResponse("plugin not found"))
            return@get
        }
        val fileName = "${plugin.id}-${plugin.version}.apk"
        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment
                .withParameter(ContentDisposition.Parameters.FileName, fileName)
                .toString()
        )
        val size = Files.size(path)
        call.respondOutputStream(ContentType.parse(APK_MIME), HttpStatusCode.OK, size) {
            withContext(Dispatchers.IO) {
                Files.newInputStream(path).use { input -> input.copyTo(this@respondOutputStream) }
            }
        }
    }
}

fun Route.adminRoutes(media: MediaRepository) {
    post("/admin/plugins") {
        val expected = System.getenv("ADMIN_TOKEN")?.takeIf { it.isNotBlank() }
        if (expected == null) {
            call.respond(HttpStatusCode.ServiceUnavailable, ErrorResponse("admin api disabled"))
            return@post
        }
        val provided = call.request.headers[ADMIN_HEADER]
        if (provided == null ||
            !MessageDigest.isEqual(expected.toByteArray(), provided.toByteArray())
        ) {
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("unauthorized"))
            return@post
        }

        val upload = try {
            call.receiveUpload("apk", MAX_APK_BYTES)
        } catch (_: TooLargeException) {
            call.respond(HttpStatusCode.PayloadTooLarge, ErrorResponse("apk too large"))
            return@post
        }

        val file = upload.file
        if (file == null) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("apk is required"))
            return@post
        }

        try {
            val pluginId = upload.fields["id"]?.trim().orEmpty()
            val version = upload.fields["version"]?.trim().orEmpty()
            val title = upload.fields["title"]?.trim().orEmpty()
            if (!PLUGIN_ID.matches(pluginId) || !PLUGIN_VERSION.matches(version) || title.isEmpty()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("id, version and title are required")
                )
                return@post
            }
            if (file.size <= 0) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("apk is empty"))
                return@post
            }

            val previous = media.pluginFile(pluginId)?.storageKey
            val storageKey = Storage.pluginKey(pluginId, version)
            Storage.commit(file, storageKey, replace = true)

            val plugin = media.publish(
                NewPlugin(
                    id = pluginId,
                    title = title,
                    description = upload.fields["description"]?.trim().orEmpty(),
                    category = upload.fields["category"]?.trim().orEmpty(),
                    version = version,
                    minAppVersion = upload.fields["minAppVersion"]?.toIntOrNull() ?: 0,
                    apkSha256 = file.sha256,
                    apkSize = file.size,
                    storageKey = storageKey,
                    iconUrl = upload.fields["iconUrl"]?.trim().orEmpty()
                )
            )
            // старая версия больше не нужна
            if (previous != null && previous != storageKey) {
                Storage.pathOrNull(previous)?.let { old ->
                    withContext(Dispatchers.IO) { Files.deleteIfExists(old) }
                }
            }
            call.respond(HttpStatusCode.Created, plugin)
        } finally {
            withContext(Dispatchers.IO) { Files.deleteIfExists(file.temp) }
        }
    }
}

// --- multipart и Range ----------------------------------------------------

private class Upload(val fields: Map<String, String>, val file: IncomingFile?, val fileMime: String?)

private suspend fun ApplicationCall.receiveUpload(fileField: String, limit: Long): Upload {
    val fields = linkedMapOf<String, String>()
    var received: IncomingFile? = null
    var mime: String? = null

    val multipart = receiveMultipart()
    while (true) {
        val part = multipart.readPart() ?: break
        try {
            when (part) {
                is PartData.FormItem -> fields[part.name.orEmpty()] = part.value
                is PartData.FileItem -> {
                    if (part.name == fileField && received == null) {
                        mime = part.contentType?.toString()
                        received = withContext(Dispatchers.IO) {
                            part.streamProvider().use { input ->
                                Storage.receive(input, limit)
                            }
                        }
                    }
                }
                else -> Unit
            }
        } finally {
            part.dispose()
        }
    }
    return Upload(fields, received, mime)
}

private sealed interface ByteRange {
    data class Partial(val start: Long, val endInclusive: Long) : ByteRange

    object Unsatisfiable : ByteRange
}

/** Поддерживает "bytes=start-end", "bytes=start-" и "bytes=-suffix"; null — отдать файл целиком. */
private fun parseRange(header: String?, size: Long): ByteRange? {
    if (header == null || !header.startsWith("bytes=")) return null
    val spec = header.removePrefix("bytes=").substringBefore(',').trim()
    val dash = spec.indexOf('-')
    if (dash < 0) return null
    val startRaw = spec.substring(0, dash).trim()
    val endRaw = spec.substring(dash + 1).trim()
    if (startRaw.isEmpty() && endRaw.isEmpty()) return null

    if (startRaw.isEmpty()) {
        val suffix = endRaw.toLongOrNull() ?: return null
        if (suffix <= 0 || size == 0L) return ByteRange.Unsatisfiable
        return ByteRange.Partial((size - suffix).coerceAtLeast(0), size - 1)
    }

    val start = startRaw.toLongOrNull() ?: return null
    val end = if (endRaw.isEmpty()) size - 1 else endRaw.toLongOrNull() ?: return null
    if (start < 0 || start >= size) return ByteRange.Unsatisfiable
    val last = minOf(end, size - 1)
    if (last < start) return ByteRange.Unsatisfiable
    return ByteRange.Partial(start, last)
}

private suspend fun OutputStream.streamRange(path: Path, start: Long, length: Long) {
    val output = this
    withContext(Dispatchers.IO) {
        java.io.RandomAccessFile(path.toFile(), "r").use { input ->
            input.seek(start)
            val buffer = ByteArray(64 * 1024)
            var remaining = length
            while (remaining > 0) {
                val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                if (read < 0) break
                output.write(buffer, 0, read)
                remaining -= read
            }
            output.flush()
        }
    }
}