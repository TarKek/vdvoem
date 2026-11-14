package app.vdvoem.server

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.security.MessageDigest
import java.security.SecureRandom
import java.sql.Connection
import java.util.UUID
import javax.sql.DataSource

object Db {
    private val random = SecureRandom()

    fun connect(): DataSource {
        val config = HikariConfig().apply {
            jdbcUrl = System.getenv("JDBC_URL") ?: "jdbc:postgresql://localhost:5432/vdvoem"
            username = System.getenv("DB_USER") ?: "vdvoem"
            password = System.getenv("DB_PASSWORD") ?: "vdvoem"
            maximumPoolSize = (System.getenv("DB_POOL") ?: "8").toInt()
            driverClassName = "org.postgresql.Driver"
        }
        val dataSource = HikariDataSource(config)
        dataSource.connection.use { connection ->
            val schema = Db::class.java.getResourceAsStream("/schema.sql")!!
                .bufferedReader().use { it.readText() }
            connection.createStatement().use { statement -> statement.execute(schema) }
        }
        return dataSource
    }

    fun newId(): String = UUID.randomUUID().toString()

    fun newToken(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hash(token: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun shortCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..8).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
    }
}

fun <T> Connection.tx(block: (Connection) -> T): T {
    val previous = autoCommit
    autoCommit = false
    return try {
        val result = block(this)
        commit()
        result
    } catch (t: Throwable) {
        rollback()
        throw t
    } finally {
        autoCommit = previous
    }
}