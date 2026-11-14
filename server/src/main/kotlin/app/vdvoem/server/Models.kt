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
data class ChangesDto(
    val pair: PairDto? = null,
    val events: List<EventDto> = emptyList(),
    val wishes: List<WishDto> = emptyList(),
    val gifts: List<GiftDto> = emptyList()
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