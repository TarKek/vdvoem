package app.vdvoem.server

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PairDto(
    val id: String,
    @SerialName("nameA") val nameA: String,
    @SerialName("nameB") val nameB: String,
    @SerialName("sinceMillis") val sinceMillis: Long,
    @SerialName("birthdayA") val birthdayA: Long? = null,
    @SerialName("birthdayB") val birthdayB: Long? = null,
    @SerialName("updatedAt") val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class EventDto(
    val id: String,
    val title: String,
    @SerialName("dateMillis") val dateMillis: Long,
    val annual: Boolean,
    @SerialName("updatedAt") val updatedAt: Long,
    @SerialName("deletedAt") val deletedAt: Long? = null
)

@Serializable
data class WishDto(
    val id: String,
    val title: String,
    val note: String = "",
    @SerialName("priceEstimate") val priceEstimate: Long? = null,
    val status: String,
    @SerialName("updatedAt") val updatedAt: Long,
    @SerialName("deletedAt") val deletedAt: Long? = null,
    /** 0 — обычный, 1 — низкий, 2 — высокий. */
    val priority: Int = 0,
    val url: String = "",
    val surprise: Boolean = false,
    /** Устройство автора: сюрпризы партнёра на клиенте скрываются. */
    @SerialName("ownerDeviceId") val ownerDeviceId: String = ""
)

@Serializable
data class GiftDto(
    val id: String,
    val title: String,
    val note: String = "",
    val budget: Long? = null,
    val status: String,
    @SerialName("updatedAt") val updatedAt: Long,
    @SerialName("deletedAt") val deletedAt: Long? = null,
    /** Желание, из которого вырос подарок. */
    @SerialName("wishId") val wishId: String? = null,
    val surprise: Boolean = false,
    val ownerDeviceId: String = "",
    val boughtAt: Long? = null,
    val givenAt: Long? = null
)

@Serializable
data class PokeDto(
    val id: String,
    val text: String,
    @SerialName("createdAt") val createdAt: Long = 0L,
    @SerialName("updatedAt") val updatedAt: Long,
    @SerialName("deletedAt") val deletedAt: Long? = null
)

@Serializable
data class ChangesDto(
    val pair: PairDto? = null,
    val events: List<EventDto> = emptyList(),
    val wishes: List<WishDto> = emptyList(),
    val gifts: List<GiftDto> = emptyList(),
    val pokes: List<PokeDto> = emptyList(),
    @SerialName("quizScores") val quizScores: List<QuizScoreDto> = emptyList(),
    val lessons: List<LessonDto> = emptyList(),
    @SerialName("cycleDays") val cycleDays: List<CycleDayDto> = emptyList(),
    @SerialName("cycleSettings") val cycleSettings: List<CycleSettingsDto> = emptyList()
)

@Serializable
data class LessonDto(
    val id: String,
    val subject: String = "",
    val weekday: Int = 1,
    @SerialName("startMinute") val startMinute: Int = 0,
    @SerialName("endMinute") val endMinute: Int = 0,
    val room: String = "",
    val teacher: String = "",
    @SerialName("updatedAt") val updatedAt: Long = 0,
    @SerialName("deletedAt") val deletedAt: Long? = null
)

@Serializable
data class QuizScoreDto(
    val id: String,
    @SerialName("quizId") val quizId: String,
    val percent: Int,
    @SerialName("passedAt") val passedAt: Long,
    @SerialName("updatedAt") val updatedAt: Long,
    @SerialName("deletedAt") val deletedAt: Long? = null
)

/**
 * День трекера цикла.
 *
 * `symptoms` — массив стабильных ключей (`cramps`, `headache`, …): в JSON обычный
 * массив, в PostgreSQL `TEXT[]`. `flow` — имя перечисления (NONE/LIGHT/MEDIUM/HEAVY).
 */
@Serializable
data class CycleDayDto(
    val id: String,
    @SerialName("dateMillis") val dateMillis: Long = 0,
    val period: Boolean = false,
    val flow: String = "NONE",
    val symptoms: List<String> = emptyList(),
    val mood: String = "",
    val weight: Float? = null,
    val note: String = "",
    @SerialName("updatedAt") val updatedAt: Long = 0,
    @SerialName("deletedAt") val deletedAt: Long? = null
)

/** Ожидания пары по циклу: одна запись на пару, `id` совпадает с `pairId`. */
@Serializable
data class CycleSettingsDto(
    val id: String,
    @SerialName("cycleLength") val cycleLength: Int = 28,
    @SerialName("periodLength") val periodLength: Int = 5,
    @SerialName("updatedAt") val updatedAt: Long = 0,
    @SerialName("deletedAt") val deletedAt: Long? = null
)

@Serializable
data class SyncResponse(
    @SerialName("serverTime") val serverTime: Long,
    val changes: ChangesDto
)

@Serializable
data class CreatePairRequest(
    @SerialName("nameA") val nameA: String,
    @SerialName("nameB") val nameB: String,
    @SerialName("sinceMillis") val sinceMillis: Long,
    @SerialName("birthdayA") val birthdayA: Long? = null,
    @SerialName("birthdayB") val birthdayB: Long? = null,
    @SerialName("deviceName") val deviceName: String = "android"
)

@Serializable
data class JoinPairRequest(
    @SerialName("inviteCode") val inviteCode: String,
    @SerialName("deviceName") val deviceName: String = "android"
)

@Serializable
data class AuthResponse(
    @SerialName("pairId") val pairId: String,
    @SerialName("deviceToken") val deviceToken: String,
    @SerialName("inviteCode") val inviteCode: String
)

@Serializable
data class PushRequest(
    @SerialName("baseTime") val baseTime: Long = 0,
    val changes: ChangesDto
)

@Serializable
data class PushResponse(
    @SerialName("serverTime") val serverTime: Long,
    val applied: Int,
    val conflicts: List<String> = emptyList()
)

@Serializable
data class HealthResponse(val status: String, val time: Long)

@Serializable
data class ErrorResponse(val error: String)

@Serializable
data class AttachmentDto(
    val id: String,
    @SerialName("entityType") val entityType: String,
    @SerialName("entityId") val entityId: String,
    val kind: String,
    val mime: String,
    val size: Long,
    val sha256: String,
    @SerialName("durationMs") val durationMs: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    @SerialName("createdAt") val createdAt: Long,
    @SerialName("updatedAt") val updatedAt: Long,
    @SerialName("deletedAt") val deletedAt: Long? = null
)

@Serializable
data class AttachmentsResponse(
    @SerialName("serverTime") val serverTime: Long,
    val attachments: List<AttachmentDto>
)

@Serializable
data class PluginDto(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val version: String,
    @SerialName("minAppVersion") val minAppVersion: Int,
    @SerialName("apkSha256") val apkSha256: String,
    @SerialName("apkSize") val apkSize: Long,
    @SerialName("iconUrl") val iconUrl: String,
    @SerialName("publishedAt") val publishedAt: Long
)