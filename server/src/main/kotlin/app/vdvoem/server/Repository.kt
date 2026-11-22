package app.vdvoem.server

import java.sql.Connection
import javax.sql.DataSource

data class SyncDevice(val pairId: String, val deviceId: String)

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
                                   invite_code, updated_at, sync_cursor, revision)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, 1)
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

    fun currentCursor(pairId: String): Long = dataSource.connection.use { connection ->
        connection.prepareStatement("SELECT sync_cursor FROM pairs WHERE id = ?").use { statement ->
            statement.setObject(1, java.util.UUID.fromString(pairId))
            statement.executeQuery().use { rows ->
                if (rows.next()) rows.getLong(1) else 0L
            }
        }
    }

    fun revokeDevice(token: String): Boolean = dataSource.connection.use { connection ->
        connection.prepareStatement("DELETE FROM devices WHERE token_hash = ?").use { statement ->
            statement.setString(1, Db.hash(token))
            statement.executeUpdate() > 0
        }
    }

    fun authenticateSyncDevice(token: String, claimedDeviceId: String): SyncDevice? =
        dataSource.connection.use { connection ->
            if (claimedDeviceId.isBlank()) {
                return@use connection.prepareStatement(
                    "SELECT pair_id, device_key FROM devices WHERE token_hash = ?"
                ).use { statement ->
                    statement.setString(1, Db.hash(token))
                    statement.executeQuery().use { rows ->
                        if (rows.next()) SyncDevice(rows.getString(1), "") else null
                    }
                }
            }
            connection.prepareStatement(
                """
                UPDATE devices SET device_key = ?
                WHERE token_hash = ? AND (device_key = '' OR device_key = ?)
                RETURNING pair_id, device_key
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, claimedDeviceId)
                statement.setString(2, Db.hash(token))
                statement.setString(3, claimedDeviceId)
                statement.executeQuery().use { rows ->
                    if (rows.next()) SyncDevice(rows.getString(1), rows.getString(2)) else null
                }
            }
        }

    fun pull(pairId: String, since: Long, viewerDeviceId: String): ChangesDto = dataSource.connection.use { connection ->
        val pair = connection.prepareStatement(
            """
            SELECT id, name_a, name_b, since_millis, birthday_a, birthday_b, updated_at
            FROM pairs WHERE id = ? AND deleted_at IS NULL AND revision > ?
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
            FROM events WHERE pair_id = ? AND revision > ? ORDER BY updated_at
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
            SELECT id, title, note, price_estimate, status, updated_at, deleted_at,
                   priority, url, surprise, owner_device_id
            FROM wishes WHERE pair_id = ? AND revision > ?
              AND (NOT surprise OR owner_device_id = ?)
            ORDER BY updated_at
            """.trimIndent()
        ).use { statement ->
            statement.setObject(1, java.util.UUID.fromString(pairId))
            statement.setLong(2, since)
            statement.setString(3, viewerDeviceId)
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
                            deletedAt = rows.getLong(7).takeUnless { rows.wasNull() },
                            priority = rows.getInt(8),
                            url = rows.getString(9).orEmpty(),
                            surprise = rows.getBoolean(10),
                            ownerDeviceId = rows.getString(11).orEmpty()
                        )
                    )
                }
            }
        }

        val gifts = connection.prepareStatement(
            """
            SELECT id, title, note, budget, status, updated_at, deleted_at, wish_id,
                   surprise, owner_device_id, bought_at, given_at
            FROM gifts WHERE pair_id = ? AND revision > ?
              AND (NOT surprise OR status = 'GIVEN' OR owner_device_id = ?)
            ORDER BY updated_at
            """.trimIndent()
        ).use { statement ->
            statement.setObject(1, java.util.UUID.fromString(pairId))
            statement.setLong(2, since)
            statement.setString(3, viewerDeviceId)
            statement.executeQuery().use { rows ->
                buildList {
                    while (rows.next()) add(
                        GiftDto(
                            id = rows.getString(1), title = rows.getString(2), note = rows.getString(3),
                            budget = rows.getLong(4).takeUnless { rows.wasNull() }, status = rows.getString(5),
                            updatedAt = rows.getLong(6), deletedAt = rows.getLong(7).takeUnless { rows.wasNull() },
                            wishId = rows.getString(8).orEmpty().ifBlank { null },
                            surprise = rows.getBoolean(9),
                            ownerDeviceId = rows.getString(10).orEmpty(),
                            boughtAt = rows.getLong(11).takeUnless { rows.wasNull() },
                            givenAt = rows.getLong(12).takeUnless { rows.wasNull() }
                        )
                    )
                }
            }
        }

        val pokes = connection.prepareStatement(
            "SELECT id, text, created_at, updated_at, deleted_at FROM pokes WHERE pair_id = ? AND revision > ? ORDER BY updated_at"
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

        val quizScores = connection.prepareStatement(
            "SELECT id, quiz_id, percent, passed_at, updated_at, deleted_at FROM quiz_scores WHERE pair_id = ? AND revision > ? ORDER BY updated_at"
        ).use { statement ->
            statement.setObject(1, java.util.UUID.fromString(pairId))
            statement.setLong(2, since)
            statement.executeQuery().use { rows ->
                buildList {
                    while (rows.next()) add(
                        QuizScoreDto(
                            id = rows.getString(1), quizId = rows.getString(2), percent = rows.getInt(3),
                            passedAt = rows.getLong(4), updatedAt = rows.getLong(5),
                            deletedAt = rows.getLong(6).takeUnless { rows.wasNull() }
                        )
                    )
                }
            }
        }

        val lessons = connection.prepareStatement(
            "SELECT id, subject, weekday, start_minute, end_minute, room, teacher, updated_at, deleted_at, textbook_by " +
                "FROM lessons WHERE pair_id = ? AND revision > ? ORDER BY weekday, start_minute"
        ).use { statement ->
            statement.setObject(1, java.util.UUID.fromString(pairId))
            statement.setLong(2, since)
            statement.executeQuery().use { rows ->
                buildList {
                    while (rows.next()) add(
                        LessonDto(
                            id = rows.getString(1), subject = rows.getString(2),
                            weekday = rows.getInt(3), startMinute = rows.getInt(4),
                            endMinute = rows.getInt(5), room = rows.getString(6),
                            teacher = rows.getString(7), updatedAt = rows.getLong(8),
                            deletedAt = rows.getLong(9).takeUnless { rows.wasNull() },
                            textbookBy = rows.getString(10) ?: ""
                        )
                    )
                }
            }
        }

        val tasks = connection.prepareStatement(
            "SELECT id, title, note, due_at, is_completed, created_by, created_at, updated_at, deleted_at, version " +
                "FROM tasks WHERE pair_id = ? AND revision > ? ORDER BY updated_at"
        ).use { statement ->
            statement.setObject(1, java.util.UUID.fromString(pairId))
            statement.setLong(2, since)
            statement.executeQuery().use { rows ->
                buildList {
                    while (rows.next()) add(
                        TaskDto(
                            id = rows.getString(1),
                            title = rows.getString(2).orEmpty(),
                            note = rows.getString(3).orEmpty(),
                            dueAt = rows.getLong(4).takeUnless { rows.wasNull() },
                            isCompleted = rows.getBoolean(5),
                            createdBy = rows.getString(6).orEmpty(),
                            createdAt = rows.getLong(7),
                            updatedAt = rows.getLong(8),
                            deletedAt = rows.getLong(9).takeUnless { rows.wasNull() },
                            version = rows.getLong(10).coerceAtLeast(1)
                        )
                    )
                }
            }
        }

        val cycleDays = connection.prepareStatement(
            "SELECT id, date_millis, period, flow, symptoms, mood, weight, note, updated_at, deleted_at " +
                "FROM cycle_days WHERE pair_id = ? AND revision > ? ORDER BY date_millis"
        ).use { statement ->
            statement.setObject(1, java.util.UUID.fromString(pairId))
            statement.setLong(2, since)
            statement.executeQuery().use { rows ->
                buildList {
                    while (rows.next()) add(
                        CycleDayDto(
                            id = rows.getString(1),
                            dateMillis = rows.getLong(2),
                            period = rows.getBoolean(3),
                            flow = rows.getString(4) ?: "NONE",
                            symptoms = rows.getArray(5).toStringList(),
                            mood = rows.getString(6).orEmpty(),
                            weight = rows.getFloat(7).takeUnless { rows.wasNull() },
                            note = rows.getString(8).orEmpty(),
                            updatedAt = rows.getLong(9),
                            deletedAt = rows.getLong(10).takeUnless { rows.wasNull() }
                        )
                    )
                }
            }
        }

        val cycleSettings = connection.prepareStatement(
            "SELECT id, cycle_length, period_length, updated_at, deleted_at " +
                "FROM cycle_settings WHERE pair_id = ? AND revision > ? ORDER BY updated_at"
        ).use { statement ->
            statement.setObject(1, java.util.UUID.fromString(pairId))
            statement.setLong(2, since)
            statement.executeQuery().use { rows ->
                buildList {
                    while (rows.next()) add(
                        CycleSettingsDto(
                            id = rows.getString(1),
                            cycleLength = rows.getInt(2),
                            periodLength = rows.getInt(3),
                            updatedAt = rows.getLong(4),
                            deletedAt = rows.getLong(5).takeUnless { rows.wasNull() }
                        )
                    )
                }
            }
        }

        ChangesDto(
            pair = pair, events = events, wishes = wishes, gifts = gifts, pokes = pokes,
            quizScores = quizScores, lessons = lessons, tasks = tasks, cycleDays = cycleDays,
            cycleSettings = cycleSettings
        )
    }

    fun push(pairId: String, deviceId: String, changes: ChangesDto): Int = dataSource.connection.use { connection ->
        connection.tx { tx ->
            // Ревизия и строки изменяются атомарно; блокировка pairs сериализует push пары.
            tx.prepareStatement(
                "UPDATE pairs SET sync_cursor = sync_cursor + 1 WHERE id = ?"
            ).use { statement ->
                statement.setObject(1, java.util.UUID.fromString(pairId))
                check(statement.executeUpdate() == 1) { "pair not found" }
            }
            var applied = 0

            changes.pair?.let { pair ->
                applied += tx.prepareStatement(
                    """
                    UPDATE pairs SET name_a = ?, name_b = ?, since_millis = ?,
                                     birthday_a = ?, birthday_b = ?, updated_at = ?, revision = sync_cursor
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
                    WHERE events.pair_id = EXCLUDED.pair_id AND EXCLUDED.updated_at > events.updated_at
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
                    INSERT INTO wishes (id, pair_id, title, note, price_estimate, status, updated_at,
                        deleted_at, priority, url, surprise, owner_device_id)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET title = EXCLUDED.title, note = EXCLUDED.note,
                        price_estimate = EXCLUDED.price_estimate, status = EXCLUDED.status,
                        updated_at = EXCLUDED.updated_at, deleted_at = EXCLUDED.deleted_at,
                        priority = EXCLUDED.priority, url = EXCLUDED.url,
                        surprise = EXCLUDED.surprise, owner_device_id = EXCLUDED.owner_device_id
                    WHERE wishes.pair_id = EXCLUDED.pair_id AND EXCLUDED.updated_at > wishes.updated_at
                      AND (NOT wishes.surprise OR wishes.owner_device_id = EXCLUDED.owner_device_id)
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
                    statement.setInt(9, wish.priority)
                    statement.setString(10, wish.url)
                    statement.setBoolean(11, wish.surprise)
                    statement.setString(12, if (wish.surprise) deviceId else "")
                    statement.executeUpdate()
                }
            }

            changes.gifts.forEach { gift ->
                applied += tx.prepareStatement(
                    """
                    INSERT INTO gifts (id, pair_id, title, note, budget, status, updated_at,
                        deleted_at, wish_id, surprise, owner_device_id, bought_at, given_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET title = EXCLUDED.title, note = EXCLUDED.note,
                        budget = EXCLUDED.budget, status = EXCLUDED.status,
                        updated_at = EXCLUDED.updated_at, deleted_at = EXCLUDED.deleted_at,
                        wish_id = EXCLUDED.wish_id, surprise = EXCLUDED.surprise,
                        owner_device_id = EXCLUDED.owner_device_id,
                        bought_at = EXCLUDED.bought_at, given_at = EXCLUDED.given_at
                    WHERE gifts.pair_id = EXCLUDED.pair_id AND EXCLUDED.updated_at > gifts.updated_at
                      AND (NOT gifts.surprise OR gifts.status = 'GIVEN' OR
                           gifts.owner_device_id = EXCLUDED.owner_device_id)
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
                    if (gift.wishId != null) statement.setString(9, gift.wishId)
                    else statement.setNull(9, java.sql.Types.VARCHAR)
                    statement.setBoolean(10, gift.surprise)
                    statement.setString(11, if (gift.surprise) deviceId else "")
                    if (gift.boughtAt != null) statement.setLong(12, gift.boughtAt)
                    else statement.setNull(12, java.sql.Types.BIGINT)
                    if (gift.givenAt != null) statement.setLong(13, gift.givenAt)
                    else statement.setNull(13, java.sql.Types.BIGINT)
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
                    WHERE pokes.pair_id = EXCLUDED.pair_id AND EXCLUDED.updated_at > pokes.updated_at
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

            changes.quizScores.forEach { score ->
                applied += tx.prepareStatement(
                    """
                    INSERT INTO quiz_scores (id, pair_id, quiz_id, percent, passed_at, updated_at, deleted_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET percent = EXCLUDED.percent,
                        passed_at = EXCLUDED.passed_at, updated_at = EXCLUDED.updated_at,
                        deleted_at = EXCLUDED.deleted_at
                    WHERE quiz_scores.pair_id = EXCLUDED.pair_id AND EXCLUDED.updated_at > quiz_scores.updated_at
                    """.trimIndent()
                ).use { statement ->
                    statement.setObject(1, java.util.UUID.fromString(score.id))
                    statement.setObject(2, java.util.UUID.fromString(pairId))
                    statement.setString(3, score.quizId)
                    statement.setInt(4, score.percent)
                    statement.setLong(5, if (score.passedAt > 0) score.passedAt else score.updatedAt)
                    statement.setLong(6, score.updatedAt)
                    if (score.deletedAt != null) statement.setLong(7, score.deletedAt)
                    else statement.setNull(7, java.sql.Types.BIGINT)
                    statement.executeUpdate()
                }
            }

            changes.lessons.forEach { lesson ->
                applied += tx.prepareStatement(
                    """
                    INSERT INTO lessons (id, pair_id, subject, weekday, start_minute, end_minute,
                        room, teacher, textbook_by, updated_at, deleted_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, COALESCE(?, ''), ?, ?)
                    ON CONFLICT (id) DO UPDATE SET subject = EXCLUDED.subject,
                        weekday = EXCLUDED.weekday, start_minute = EXCLUDED.start_minute,
                        end_minute = EXCLUDED.end_minute, room = EXCLUDED.room,
                        teacher = EXCLUDED.teacher,
                        textbook_by = COALESCE(?, lessons.textbook_by),
                        updated_at = EXCLUDED.updated_at,
                        deleted_at = EXCLUDED.deleted_at
                    WHERE lessons.pair_id = EXCLUDED.pair_id AND EXCLUDED.updated_at > lessons.updated_at
                    """.trimIndent()
                ).use { statement ->
                    statement.setObject(1, java.util.UUID.fromString(lesson.id))
                    statement.setObject(2, java.util.UUID.fromString(pairId))
                    statement.setString(3, lesson.subject)
                    statement.setInt(4, lesson.weekday)
                    statement.setInt(5, lesson.startMinute)
                    statement.setInt(6, lesson.endMinute)
                    statement.setString(7, lesson.room)
                    statement.setString(8, lesson.teacher)
                    statement.setString(9, lesson.textbookBy)
                    statement.setLong(10, lesson.updatedAt)
                    if (lesson.deletedAt != null) statement.setLong(11, lesson.deletedAt)
                    else statement.setNull(11, java.sql.Types.BIGINT)
                    statement.setString(12, lesson.textbookBy)
                    statement.executeUpdate()
                }
            }

            changes.tasks.forEach { task ->
                applied += tx.prepareStatement(
                    """
                    INSERT INTO tasks (id, pair_id, title, note, due_at, is_completed,
                        created_by, created_at, updated_at, deleted_at, version)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET title = EXCLUDED.title,
                        note = EXCLUDED.note, due_at = EXCLUDED.due_at,
                        is_completed = EXCLUDED.is_completed, created_by = EXCLUDED.created_by,
                        created_at = EXCLUDED.created_at, updated_at = EXCLUDED.updated_at,
                        deleted_at = EXCLUDED.deleted_at, version = EXCLUDED.version
                    WHERE tasks.pair_id = EXCLUDED.pair_id AND EXCLUDED.updated_at > tasks.updated_at
                    """.trimIndent()
                ).use { statement ->
                    statement.setObject(1, java.util.UUID.fromString(task.id))
                    statement.setObject(2, java.util.UUID.fromString(pairId))
                    statement.setString(3, task.title)
                    statement.setString(4, task.note)
                    if (task.dueAt != null) statement.setLong(5, task.dueAt)
                    else statement.setNull(5, java.sql.Types.BIGINT)
                    statement.setBoolean(6, task.isCompleted)
                    statement.setString(7, task.createdBy)
                    statement.setLong(8, if (task.createdAt > 0) task.createdAt else task.updatedAt)
                    statement.setLong(9, task.updatedAt)
                    if (task.deletedAt != null) statement.setLong(10, task.deletedAt)
                    else statement.setNull(10, java.sql.Types.BIGINT)
                    statement.setLong(11, task.version.coerceAtLeast(1))
                    statement.executeUpdate()
                }
            }

            changes.cycleDays.forEach { day ->
                applied += tx.prepareStatement(
                    """
                    INSERT INTO cycle_days (id, pair_id, date_millis, period, flow, symptoms, mood,
                        weight, note, updated_at, deleted_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET date_millis = EXCLUDED.date_millis,
                        period = EXCLUDED.period, flow = EXCLUDED.flow,
                        symptoms = EXCLUDED.symptoms, mood = EXCLUDED.mood,
                        weight = EXCLUDED.weight, note = EXCLUDED.note,
                        updated_at = EXCLUDED.updated_at, deleted_at = EXCLUDED.deleted_at
                    WHERE cycle_days.pair_id = EXCLUDED.pair_id AND EXCLUDED.updated_at > cycle_days.updated_at
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, day.id)
                    statement.setObject(2, java.util.UUID.fromString(pairId))
                    statement.setLong(3, day.dateMillis)
                    statement.setBoolean(4, day.period)
                    statement.setString(5, day.flow)
                    statement.setArray(6, tx.createArrayOf("text", day.symptoms.toTypedArray()))
                    statement.setString(7, day.mood)
                    if (day.weight != null) statement.setFloat(8, day.weight)
                    else statement.setNull(8, java.sql.Types.REAL)
                    statement.setString(9, day.note)
                    statement.setLong(10, day.updatedAt)
                    if (day.deletedAt != null) statement.setLong(11, day.deletedAt)
                    else statement.setNull(11, java.sql.Types.BIGINT)
                    statement.executeUpdate()
                }
            }

            changes.cycleSettings.forEach { settings ->
                applied += tx.prepareStatement(
                    """
                    INSERT INTO cycle_settings (id, pair_id, cycle_length, period_length,
                        updated_at, deleted_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET cycle_length = EXCLUDED.cycle_length,
                        period_length = EXCLUDED.period_length,
                        updated_at = EXCLUDED.updated_at, deleted_at = EXCLUDED.deleted_at
                    WHERE cycle_settings.pair_id = EXCLUDED.pair_id AND EXCLUDED.updated_at > cycle_settings.updated_at
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, settings.id)
                    statement.setObject(2, java.util.UUID.fromString(pairId))
                    statement.setInt(3, settings.cycleLength.coerceAtLeast(1))
                    statement.setInt(4, settings.periodLength.coerceAtLeast(1))
                    statement.setLong(5, settings.updatedAt)
                    if (settings.deletedAt != null) statement.setLong(6, settings.deletedAt)
                    else statement.setNull(6, java.sql.Types.BIGINT)
                    statement.executeUpdate()
                }
            }

            applied
        }
    }

    /** PostgreSQL `TEXT[]` → список строк. NULL и NULL-элементы пропускаются. */
    private fun java.sql.Array?.toStringList(): List<String> {
        val values = this?.array as? Array<*> ?: return emptyList()
        return values.mapNotNull { it as? String }
    }
}