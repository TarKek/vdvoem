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
    @SerialName("deletedAt") val deletedAt: Long? = null
)

@Serializable
data class GiftDto(
    val id: String,
    val title: String,
    val note: String = "",
    val budget: Long? = null,
    val status: String,
    @SerialName("updatedAt") val updatedAt: Long,
    @SerialName("deletedAt") val deletedAt: Long? = null
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
    val pokes: List<PokeDto> = emptyList()
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