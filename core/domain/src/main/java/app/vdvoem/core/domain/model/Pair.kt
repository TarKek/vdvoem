package app.vdvoem.core.domain.model

/**
 * Пара пользователей. Пока хранится только одна локальная пара, но модель готова
 * к синхронизации: стабильный id, метки времени и мягкое удаление на уровне БД.
 */
data class Pair(
    val id: String,
    val nameA: String,
    val nameB: String,
    val sinceMillis: Long,
    val birthdayA: Long? = null,
    val birthdayB: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
