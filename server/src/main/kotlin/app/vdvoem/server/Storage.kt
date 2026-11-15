package app.vdvoem.server

import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

private const val BUFFER_SIZE = 64 * 1024

class TooLargeException(val limit: Long) : RuntimeException("file is larger than $limit bytes")

/** Временный файл, уже посчитанный sha256 и размер. */
class IncomingFile(val temp: Path, val sha256: String, val size: Long)

/**
 * Файловое хранилище: <STORAGE_DIR>/attachments/<sha256> и <STORAGE_DIR>/plugins/<id>/<version>.apk.
 * STORAGE_DIR по умолчанию /data/storage (volume в docker-compose), создаётся при старте.
 */
object Storage {
    private val root: Path = Paths.get(
        System.getenv("STORAGE_DIR")?.takeIf { it.isNotBlank() } ?: "/data/storage"
    ).toAbsolutePath().normalize()

    fun ensure() {
        listOf("", "tmp", "attachments", "plugins").forEach { sub ->
            val dir = if (sub.isEmpty()) root else root.resolve(sub)
            if (!Files.isDirectory(dir)) {
                Files.createDirectories(dir)
            }
            check(Files.isWritable(dir)) { "storage directory is not writable: $dir" }
        }
    }

    /** Полный путь по storage_key; ключ не может выйти за пределы каталога хранения. */
    fun path(key: String): Path {
        val target = root.resolve(key).normalize()
        require(target.startsWith(root) && target != root) { "invalid storage key" }
        return target
    }

    fun pathOrNull(key: String): Path? = runCatching { path(key) }.getOrNull()

    fun attachmentKey(sha256: String): String = "attachments/$sha256"

    fun pluginKey(pluginId: String, version: String): String = "plugins/$pluginId/$version.apk"

    /** Копирует поток в tmp-файл, считая sha256; бросает TooLargeException при превышении лимита. */
    fun receive(input: InputStream, limit: Long): IncomingFile {
        ensure()
        val temp = Files.createTempFile(root.resolve("tmp"), "upload-", ".part")
        var size = 0L
        val digest = MessageDigest.getInstance("SHA-256")
        try {
            Files.newOutputStream(temp).buffered().use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    size += read
                    if (size > limit) throw TooLargeException(limit)
                    digest.update(buffer, 0, read)
                    output.write(buffer, 0, read)
                }
            }
        } catch (t: Throwable) {
            Files.deleteIfExists(temp)
            throw t
        }
        return IncomingFile(temp, digest.digest().toHex(), size)
    }

    /**
     * Переносит файл в хранилище.
     * [replace] = false — дедупликация: если файл с таким ключом уже есть, он не перезаписывается.
     */
    fun commit(incoming: IncomingFile, key: String, replace: Boolean): Path {
        val target = path(key)
        target.parent?.let { Files.createDirectories(it) }
        if (!replace && Files.isRegularFile(target)) {
            Files.deleteIfExists(incoming.temp)
            return target
        }
        Files.move(incoming.temp, target, StandardCopyOption.REPLACE_EXISTING)
        return target
    }
}

fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }