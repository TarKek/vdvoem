package app.vdvoem.server

import java.sql.Connection
import javax.sql.DataSource

class Repository(private val dataSource: DataSource) {

    fun authenticate(token: String): String? =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT pair_id FROM devices WHERE token_hash = ?"
            ).use { statement ->
                statement.setString(1, Db.hash(token))
                statement.executeQuery().use { rows ->
                    if (rows.next()) rows.getString(1) else null
                }
            }
        }

    fun createPair(
        request: CreatePairRequest
    ): Triple<String, String, String> = dataSource.connection.use { connection ->
        connection.tx { tx ->
            val pairId = Db.newId()
            val inviteCode = Db.shortCode()
            val now = System.currentTimeMillis()
            tx.prepareStatement(
                """
                INSERT INTO pairs (id, name_a, name_b, since_millis, birthday_a, birthday_b,
                                   invite_code, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { statement ->
                statement.setObject(1, java.util.UUID.fromString(pairId))
                statement.setString(2, request.nameA)
                statement.setString(3, request.nameB)
                statement.setLong(4, request.sinceMillis)
                if (request.birthdayA != null) statement.setLong(5, request.birthdayA)
                else statement.setNull(5, java.sql.Types.BIGINT)
                if (request.birthdayB != null) statement.setLong(6, request.birthdayB)
                else statement.setNull(6, java.sql.Types.BIGINT)
                statement.setString(7, inviteCode)
                statement.setLong(8, now)
                statement.executeUpdate()
            }
            val token = addDevice(tx, pairId, request.deviceName)
            Triple(pairId, token, inviteCode)
        }
    }

    fun join(inviteCode: String): Pair<String, String>? = dataSource.connection.use { connection ->
        connection.tx { tx ->
            val pairId = tx.prepareStatement(
                "SELECT id FROM pairs WHERE invite_code = ? AND deleted_at IS NULL"
            ).use { statement ->
                statement.setString(1, inviteCode.trim().uppercase())
                statement.executeQuery().use { rows ->
                    if (rows.next()) rows.getString(1) else null
                }
            } ?: return@tx null
            pairId to addDevice(tx, pairId, "android")
        }
    }

    private fun addDevice(connection: Connection, pairId: String, deviceName: String): String {
        val token = Db.newToken()
        connection.prepareStatement(
            "INSERT INTO devices (id, pair_id, token_hash, name, created_at) VALUES (?, ?, ?, ?, ?)"
        ).use { statement ->
            statement.setObject(1, java.util.UUID.fromString(Db.newId()))
            statement.setObject(2, java.util.UUID.fromString(pairId))
            statement.setString(3, Db.hash(token))
            statement.setString(4, deviceName)
            statement.setLong(5, System.currentTimeMillis())
            statement.executeUpdate()
        }
        return token
    }

    fun pull(pairId: String, since: Long): ChangesDto = dataSource.connection.use { connection ->
        val pair = connection.prepareStatement(
            """
            SELECT id, name_a, name_b, since_millis, birthday_a, birthday_b, updated_at
            FROM pairs WHERE id = ? AND deleted_at IS NULL AND updated_at > ?
            """.trimIndent()
        ).use { statement ->
            statement.setObject(1, java.util.UUID.fromString(pairId))
            statement.setLong(2, since)
            statement.executeQuery().use { rows ->
                if (!rows.next()) null else PairDto(
                    id = rows.getString(1),
                    nameA = rows.getString(2),
                    nameB = rows.getString(3),
                    sinceMillis = rows.getLong(4),
                    birthdayA = rows.getLong(5).takeUnless { rows.wasNull() },
                    birthdayB = rows.getLong(6).takeUnless { rows.wasNull() },
                    updatedAt = rows.getLong(7)
                )
            }
        }

        val events = connection.prepareStatement(
            """
            SELECT id, title, date_millis, annual, updated_at, deleted_at
            FROM events WHERE pair_id = ? AND updated_at > ? ORDER BY updated_at
            """.trimIndent()
        ).use { statement ->
            statement.setObject(1, java.util.UUID.fromString(pairId))
            statement.setLong(2, since)
            statement.executeQuery().use { rows ->
                buildList {
                    while (rows.next()) add(
                        EventDto(
                            id = rows.getString(1),
                            title = rows.getString(2),
                            dateMillis = rows.getLong(3),
                            annual = rows.getBoolean(4),
                            updatedAt = rows.getLong(5),
                            deletedAt = rows.getLong(6).takeUnless { rows.wasNull() }
                        )
                    )
                }
            }
        }

        val wishes = connection.prepareStatement(
            """
            SELECT id, title, note, price_estimate, status, updated_at, deleted_at
            FROM wishes WHERE pair_id = ? AND updated_at > ? ORDER BY updated_at
            """.trimIndent()
        ).use { statement ->
            statement.setObject(1, java.util.UUID.fromString(pairId))
            statement.setLong(2, since)
            statement.executeQuery().use { rows ->
                buildList {
                    while (rows.next()) add(
                        WishDto(
                            id = rows.getString(1),
                            title = rows.getString(2),
                            note = rows.getString(3),
                            priceEstimate = rows.getLong(4).takeUnless { rows.wasNull() },
                            status = rows.getString(5),
                            updatedAt = rows.getLong(6),
                            deletedAt = rows.getLong(7).takeUnless { rows.wasNull() }
                        )
                    )
                }
            }
        }

        val gifts = connection.prepareStatement(
            """
            SELECT id, title, note, budget, status, updated_at, deleted_at
            FROM gifts WHERE pair_id = ? AND updated_at > ? ORDER BY updated_at
            """.trimIndent()
        ).use { statement ->
            statement.setObject(1, java.util.UUID.fromString(pairId))
            statement.setLong(2, since)
            statement.executeQuery().use { rows ->
                buildList {
                    while (rows.next()) add(
                        GiftDto(
                            id = rows.getString(1), title = rows.getString(2), note = rows.getString(3),
                            budget = rows.getLong(4).takeUnless { rows.wasNull() }, status = rows.getString(5),
                            updatedAt = rows.getLong(6), deletedAt = rows.getLong(7).takeUnless { rows.wasNull() }
                        )
                    )
                }
            }
        }

        val pokes = connection.prepareStatement(
            "SELECT id, text, created_at, updated_at, deleted_at FROM pokes WHERE pair_id = ? AND updated_at > ? ORDER BY updated_at"
        ).use { statement ->
            statement.setObject(1, java.util.UUID.fromString(pairId))
            statement.setLong(2, since)
            statement.executeQuery().use { rows ->
                buildList {
                    while (rows.next()) add(
                        PokeDto(
                            id = rows.getString(1), text = rows.getString(2), createdAt = rows.getLong(3),
                            updatedAt = rows.getLong(4), deletedAt = rows.getLong(5).takeUnless { rows.wasNull() }
                        )
                    )
                }
            }
        }

        ChangesDto(pair = pair, events = events, wishes = wishes, gifts = gifts, pokes = pokes)
    }

    fun push(pairId: String, changes: ChangesDto): Int = dataSource.connection.use { connection ->
        connection.tx { tx ->
            var applied = 0

            changes.pair?.let { pair ->
                applied += tx.prepareStatement(
                    """
                    UPDATE pairs SET name_a = ?, name_b = ?, since_millis = ?,
                                     birthday_a = ?, birthday_b = ?, updated_at = ?
                    WHERE id = ? AND updated_at < ?
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, pair.nameA)
                    statement.setString(2, pair.nameB)
                    statement.setLong(3, pair.sinceMillis)
                    if (pair.birthdayA != null) statement.setLong(4, pair.birthdayA)
                    else statement.setNull(4, java.sql.Types.BIGINT)
                    if (pair.birthdayB != null) statement.setLong(5, pair.birthdayB)
                    else statement.setNull(5, java.sql.Types.BIGINT)
                    statement.setLong(6, pair.updatedAt)
                    statement.setObject(7, java.util.UUID.fromString(pairId))
                    statement.setLong(8, pair.updatedAt)
                    statement.executeUpdate()
                }
            }

            changes.events.forEach { event ->
                applied += tx.prepareStatement(
                    """
                    INSERT INTO events (id, pair_id, title, date_millis, annual, updated_at, deleted_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET title = EXCLUDED.title,
                        date_millis = EXCLUDED.date_millis, annual = EXCLUDED.annual,
                        updated_at = EXCLUDED.updated_at, deleted_at = EXCLUDED.deleted_at
                    WHERE EXCLUDED.updated_at > events.updated_at
                    """.trimIndent()
                ).use { statement ->
                    statement.setObject(1, java.util.UUID.fromString(event.id))
                    statement.setObject(2, java.util.UUID.fromString(pairId))
                    statement.setString(3, event.title)
                    statement.setLong(4, event.dateMillis)
                    statement.setBoolean(5, event.annual)
                    statement.setLong(6, event.updatedAt)
                    if (event.deletedAt != null) statement.setLong(7, event.deletedAt)
                    else statement.setNull(7, java.sql.Types.BIGINT)
                    statement.executeUpdate()
                }
            }

            changes.wishes.forEach { wish ->
                applied += tx.prepareStatement(
                    """
                    INSERT INTO wishes (id, pair_id, title, note, price_estimate, status, updated_at, deleted_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET title = EXCLUDED.title, note = EXCLUDED.note,
                        price_estimate = EXCLUDED.price_estimate, status = EXCLUDED.status,
                        updated_at = EXCLUDED.updated_at, deleted_at = EXCLUDED.deleted_at
                    WHERE EXCLUDED.updated_at > wishes.updated_at
                    """.trimIndent()
                ).use { statement ->
                    statement.setObject(1, java.util.UUID.fromString(wish.id))
                    statement.setObject(2, java.util.UUID.fromString(pairId))
                    statement.setString(3, wish.title)
                    statement.setString(4, wish.note)
                    if (wish.priceEstimate != null) statement.setLong(5, wish.priceEstimate)
                    else statement.setNull(5, java.sql.Types.BIGINT)
                    statement.setString(6, wish.status)
                    statement.setLong(7, wish.updatedAt)
                    if (wish.deletedAt != null) statement.setLong(8, wish.deletedAt)
                    else statement.setNull(8, java.sql.Types.BIGINT)
                    statement.executeUpdate()
                }
            }

            changes.gifts.forEach { gift ->
                applied += tx.prepareStatement(
                    """
                    INSERT INTO gifts (id, pair_id, title, note, budget, status, updated_at, deleted_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET title = EXCLUDED.title, note = EXCLUDED.note,
                        budget = EXCLUDED.budget, status = EXCLUDED.status,
                        updated_at = EXCLUDED.updated_at, deleted_at = EXCLUDED.deleted_at
                    WHERE EXCLUDED.updated_at > gifts.updated_at
                    """.trimIndent()
                ).use { statement ->
                    statement.setObject(1, java.util.UUID.fromString(gift.id))
                    statement.setObject(2, java.util.UUID.fromString(pairId))
                    statement.setString(3, gift.title)
                    statement.setString(4, gift.note)
                    if (gift.budget != null) statement.setLong(5, gift.budget)
                    else statement.setNull(5, java.sql.Types.BIGINT)
                    statement.setString(6, gift.status)
                    statement.setLong(7, gift.updatedAt)
                    if (gift.deletedAt != null) statement.setLong(8, gift.deletedAt)
                    else statement.setNull(8, java.sql.Types.BIGINT)
                    statement.executeUpdate()
                }
            }

            changes.pokes.forEach { poke ->
                applied += tx.prepareStatement(
                    """
                    INSERT INTO pokes (id, pair_id, text, created_at, updated_at, deleted_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET text = EXCLUDED.text,
                        created_at = EXCLUDED.created_at, updated_at = EXCLUDED.updated_at,
                        deleted_at = EXCLUDED.deleted_at
                    WHERE EXCLUDED.updated_at > pokes.updated_at
                    """.trimIndent()
                ).use { statement ->
                    statement.setObject(1, java.util.UUID.fromString(poke.id))
                    statement.setObject(2, java.util.UUID.fromString(pairId))
                    statement.setString(3, poke.text)
                    statement.setLong(4, if (poke.createdAt > 0) poke.createdAt else poke.updatedAt)
                    statement.setLong(5, poke.updatedAt)
                    if (poke.deletedAt != null) statement.setLong(6, poke.deletedAt)
                    else statement.setNull(6, java.sql.Types.BIGINT)
                    statement.executeUpdate()
                }
            }

            applied
        }
    }
}