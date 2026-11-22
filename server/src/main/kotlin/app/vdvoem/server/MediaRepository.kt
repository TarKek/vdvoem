package app.vdvoem.server

import java.sql.ResultSet
import java.sql.Types
import java.util.UUID
import javax.sql.DataSource

data class NewAttachment(
    val entityType: String,
    val entityId: String,
    val kind: String,
    val mime: String,
    val size: Long,
    val sha256: String,
    val storageKey: String,
    val durationMs: Long? = null,
    val width: Int? = null,
    val height: Int? = null
)

data class AttachmentFile(
    val mime: String,
    val storageKey: String,
    val size: Long,
    val sha256: String
)

data class NewPlugin(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val version: String,
    val minAppVersion: Int,
    val apkSha256: String,
    val apkSize: Long,
    val storageKey: String,
    val iconUrl: String
)

data class PluginFile(
    val id: String,
    val version: String,
    val storageKey: String
)

/** Метаданные вложений и каталог плагинов; синк вложений — по updated_at, как у остальных сущностей. */
class MediaRepository(private val dataSource: DataSource) {

    fun attachments(pairId: String, since: Long): List<AttachmentDto> =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT id, entity_type, entity_id, kind, mime, size, sha256, duration_ms,
                       width, height, created_at, updated_at, deleted_at
                FROM attachments WHERE pair_id = ? AND updated_at > ? ORDER BY updated_at
                """.trimIndent()
            ).use { statement ->
                statement.setObject(1, UUID.fromString(pairId))
                statement.setLong(2, since)
                statement.executeQuery().use { rows ->
                    buildList { while (rows.next()) add(rows.toAttachment()) }
                }
            }
        }

    fun attachmentFile(pairId: String, id: String): AttachmentFile? =
        uuidOrNull(pairId)?.let { pair ->
            uuidOrNull(id)?.let { attachmentId ->
                dataSource.connection.use { connection ->
                    connection.prepareStatement(
                        """
                        SELECT mime, storage_key, size, sha256 FROM attachments
                        WHERE id = ? AND pair_id = ? AND deleted_at IS NULL
                        """.trimIndent()
                    ).use { statement ->
                        statement.setObject(1, attachmentId)
                        statement.setObject(2, pair)
                        statement.executeQuery().use { rows ->
                            if (!rows.next()) null else AttachmentFile(
                                mime = rows.getString(1),
                                storageKey = rows.getString(2),
                                size = rows.getLong(3),
                                sha256 = rows.getString(4)
                            )
                        }
                    }
                }
            }
        }

    /** Сохраняет метаданные каждой загрузки отдельной записью. */
    fun saveAttachment(pairId: String, attachment: NewAttachment): AttachmentDto =
        dataSource.connection.use { connection ->
            connection.tx { tx ->
                val entityId = UUID.fromString(attachment.entityId)
                val id = UUID.fromString(Db.newId())
                val now = System.currentTimeMillis()
                tx.prepareStatement(
                    """
                    INSERT INTO attachments (id, pair_id, entity_type, entity_id, kind, mime, size, sha256,
                                             storage_key, duration_ms, width, height, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                ).use { statement ->
                    statement.setObject(1, id)
                    statement.setObject(2, UUID.fromString(pairId))
                    statement.setString(3, attachment.entityType)
                    statement.setObject(4, entityId)
                    statement.setString(5, attachment.kind)
                    statement.setString(6, attachment.mime)
                    statement.setLong(7, attachment.size)
                    statement.setString(8, attachment.sha256)
                    statement.setString(9, attachment.storageKey)
                    if (attachment.durationMs != null) statement.setLong(10, attachment.durationMs)
                    else statement.setNull(10, Types.BIGINT)
                    if (attachment.width != null) statement.setInt(11, attachment.width)
                    else statement.setNull(11, Types.INTEGER)
                    if (attachment.height != null) statement.setInt(12, attachment.height)
                    else statement.setNull(12, Types.INTEGER)
                    statement.setLong(13, now)
                    statement.setLong(14, now)
                    statement.executeUpdate()
                }
                AttachmentDto(
                    id = id.toString(),
                    entityType = attachment.entityType,
                    entityId = attachment.entityId,
                    kind = attachment.kind,
                    mime = attachment.mime,
                    size = attachment.size,
                    sha256 = attachment.sha256,
                    durationMs = attachment.durationMs,
                    width = attachment.width,
                    height = attachment.height,
                    createdAt = now,
                    updatedAt = now
                )
            }
        }

    /** Soft-delete: файл на диске остаётся. */
    fun deleteAttachment(pairId: String, id: String): Boolean =
        uuidOrNull(pairId)?.let { pair ->
            uuidOrNull(id)?.let { attachmentId ->
                dataSource.connection.use { connection ->
                    val now = System.currentTimeMillis()
                    connection.prepareStatement(
                        """
                        UPDATE attachments SET deleted_at = ?, updated_at = ?
                        WHERE id = ? AND pair_id = ? AND deleted_at IS NULL
                        """.trimIndent()
                    ).use { statement ->
                        statement.setLong(1, now)
                        statement.setLong(2, now)
                        statement.setObject(3, attachmentId)
                        statement.setObject(4, pair)
                        statement.executeUpdate() > 0
                    }
                }
            }
        } ?: false

    fun plugins(): List<PluginDto> = dataSource.connection.use { connection ->
        connection.prepareStatement(
            """
            SELECT id, title, description, category, version, min_app_version,
                   apk_sha256, apk_size, icon_url, published_at
            FROM plugins WHERE deleted_at IS NULL ORDER BY published_at DESC
            """.trimIndent()
        ).use { statement ->
            statement.executeQuery().use { rows ->
                buildList { while (rows.next()) add(rows.toPlugin()) }
            }
        }
    }

    fun pluginFile(id: String): PluginFile? = dataSource.connection.use { connection ->
        connection.prepareStatement(
            "SELECT id, version, storage_key FROM plugins WHERE id = ? AND deleted_at IS NULL"
        ).use { statement ->
            statement.setString(1, id)
            statement.executeQuery().use { rows ->
                if (!rows.next()) null else PluginFile(
                    id = rows.getString(1),
                    version = rows.getString(2),
                    storageKey = rows.getString(3)
                )
            }
        }
    }

    /** Публикация/обновление плагина: одна строка на плагин, хранит последнюю версию. */
    fun publish(plugin: NewPlugin): PluginDto = dataSource.connection.use { connection ->
        val now = System.currentTimeMillis()
        connection.prepareStatement(
            """
            INSERT INTO plugins (id, title, description, category, version, min_app_version,
                                 apk_sha256, apk_size, storage_key, icon_url, published_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET title = EXCLUDED.title,
                description = EXCLUDED.description, category = EXCLUDED.category,
                version = EXCLUDED.version, min_app_version = EXCLUDED.min_app_version,
                apk_sha256 = EXCLUDED.apk_sha256, apk_size = EXCLUDED.apk_size,
                storage_key = EXCLUDED.storage_key, icon_url = EXCLUDED.icon_url,
                published_at = EXCLUDED.published_at, deleted_at = NULL
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, plugin.id)
            statement.setString(2, plugin.title)
            statement.setString(3, plugin.description)
            statement.setString(4, plugin.category)
            statement.setString(5, plugin.version)
            statement.setInt(6, plugin.minAppVersion)
            statement.setString(7, plugin.apkSha256)
            statement.setLong(8, plugin.apkSize)
            statement.setString(9, plugin.storageKey)
            statement.setString(10, plugin.iconUrl)
            statement.setLong(11, now)
            statement.executeUpdate()
        }
        PluginDto(
            id = plugin.id,
            title = plugin.title,
            description = plugin.description,
            category = plugin.category,
            version = plugin.version,
            minAppVersion = plugin.minAppVersion,
            apkSha256 = plugin.apkSha256,
            apkSize = plugin.apkSize,
            iconUrl = plugin.iconUrl,
            publishedAt = now
        )
    }

    private fun ResultSet.toAttachment() = AttachmentDto(
        id = getString("id"),
        entityType = getString("entity_type"),
        entityId = getString("entity_id"),
        kind = getString("kind"),
        mime = getString("mime"),
        size = getLong("size"),
        sha256 = getString("sha256"),
        durationMs = getLong("duration_ms").takeUnless { wasNull() },
        width = getInt("width").takeUnless { wasNull() },
        height = getInt("height").takeUnless { wasNull() },
        createdAt = getLong("created_at"),
        updatedAt = getLong("updated_at"),
        deletedAt = getLong("deleted_at").takeUnless { wasNull() }
    )

    private fun ResultSet.toPlugin() = PluginDto(
        id = getString("id"),
        title = getString("title"),
        description = getString("description"),
        category = getString("category"),
        version = getString("version"),
        minAppVersion = getInt("min_app_version"),
        apkSha256 = getString("apk_sha256"),
        apkSize = getLong("apk_size"),
        iconUrl = getString("icon_url"),
        publishedAt = getLong("published_at")
    )
}

internal fun uuidOrNull(value: String): UUID? = runCatching { UUID.fromString(value) }.getOrNull()