package app.vdvoem.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/** Доступное обновление: версия и прямая ссылка на APK из GitHub Releases. */
data class UpdateInfo(
    val version: String,
    val apkUrl: String,
    val notes: String = ""
)

/**
 * Проверка обновлений через GitHub Releases и установка APK.
 *
 * Репозиторий публичный, поэтому токен не нужен. Сравнение версий — по числам,
 * «0.10.0» новее «0.9.0» (обычное сравнение строк здесь ошибается).
 */
object UpdateChecker {

    private const val API = "https://api.github.com/repos/TarKek/vdvoem/releases/latest"
    private const val APK_SUFFIX = ".apk"

    /** Возвращает обновление, если на GitHub лежит версия новее [currentVersion]. */
    suspend fun check(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        val connection = (URL(API).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "vdvoem-android")
        }
        val body = try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }

        val json = JSONObject(body)
        val tag = json.optString("tag_name").removePrefix("v").removePrefix("V")
        if (tag.isBlank() || !isNewer(tag, currentVersion)) return@withContext null

        val assets = json.optJSONArray("assets")
        val apkUrl = buildList {
            for (index in 0 until (assets?.length() ?: 0)) {
                val asset = assets!!.getJSONObject(index)
                val name = asset.optString("name")
                if (name.endsWith(APK_SUFFIX, ignoreCase = true)) {
                    add(asset.optString("browser_download_url"))
                }
            }
        }.firstOrNull { it.endsWith("-release.apk", ignoreCase = true) } ?: return@withContext null

        UpdateInfo(version = tag, apkUrl = apkUrl, notes = json.optString("body"))
    }

    /** true, если [candidate] новее [current] (сравнение по числам, а не по строкам). */
    fun isNewer(candidate: String, current: String): Boolean {
        val left = parse(candidate)
        val right = parse(current)
        for (index in 0 until maxOf(left.size, right.size)) {
            val a = left.getOrElse(index) { 0 }
            val b = right.getOrElse(index) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private fun parse(version: String): List<Int> = version
        .split('.', '-', '+')
        .map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }

    /** Скачивает APK в кэш приложения и возвращает файл. */
    suspend fun download(context: Context, url: String): File = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        val id = UUID.randomUUID().toString()
        val target = File(directory, "vdvoem-update-$id.apk")
        val partial = File(directory, "$id.part")
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 15_000
            readTimeout = 60_000
            setRequestProperty("User-Agent", "vdvoem-android")
        }
        try {
            connection.inputStream.use { input ->
                partial.outputStream().use { output -> input.copyTo(output) }
            }
            if (!partial.renameTo(target)) throw java.io.IOException("Не удалось завершить загрузку APK")
            target
        } catch (error: Throwable) {
            partial.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    /** Разрешена ли установка APK из этого приложения. */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()

    /** Экран «Установка неизвестных приложений» для нашего пакета. */
    fun requestInstallPermission(context: Context) {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}