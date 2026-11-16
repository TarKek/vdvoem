package app.vdvoem.core.sync

import kotlinx.serialization.Serializable

/**
 * DTO контракта `/v1/sync` (docs/SYNC_API.md).
 *
 * Правило расширения: новое поле — optional с дефолтом, новая сущность — новый ключ
 * в [ChangesDto]. Неизвестные ключи игнорируются при разборе (Json.ignoreUnknownKeys).
 * Все поля, кроме обязательных контрактом, имеют дефолты: сервер может не прислать их.
 */
@Serializable
data class ChangesDto(
    val pair: PairDto? = null,
    val events: List<EventDto> = emptyList(),
    val wishes: List<WishDto> = emptyList(),
    val gifts: List<GiftDto> = emptyList(),
    val pokes: List<PokeDto> = emptyList(),
    val quizScores: List<QuizScoreDto> = emptyList(),
    val lessons: List<LessonDto> = emptyList(),
    /** Серверное время ответа — из него берётся следующий `since`. */
    val serverTime: Long = 0L
) {
    val size: Int
        get() = (if (pair != null) 1 else 0) + events.size + wishes.size + gifts.size + pokes.size +
            quizScores.size + lessons.size

    val isEmpty: Boolean
        get() = size == 0
}

@Serializable
data class PairDto(
    val id: String,
    val nameA: String = "",
    val nameB: String = "",
    val sinceMillis: Long = 0L,
    val birthdayA: Long? = null,
    val birthdayB: Long? = null,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null
)

@Serializable
data class EventDto(
    val id: String,
    val title: String,
    val dateMillis: Long,
    val annual: Boolean = false,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
    // ниже — расширения домена; сервер их сохраняет как есть (неизвестные ключи разрешены)
    val pairId: String? = null
)

@Serializable
data class WishDto(
    val id: String,
    val title: String,
    val status: String = "WANTED",
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
    val pairId: String? = null,
    val note: String = "",
    val priceEstimate: Long? = null,
    /** 0 — обычный, 1 — низкий, 2 — высокий. */
    val priority: Int = 0,
    val url: String = "",
    val surprise: Boolean = false,
    /** Устройство автора: по нему партнёр понимает, что сюрприз чужой. */
    val ownerDeviceId: String = ""
)

@Serializable
data class GiftDto(
    val id: String,
    val title: String,
    val status: String = "IDEA",
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
    val pairId: String? = null,
    val note: String = "",
    val budget: Long? = null,
    /** Ссылка на желание, из которого вырос подарок. */
    val wishId: String? = null
)

@Serializable
data class PokeDto(
    val id: String,
    val text: String,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
    val pairId: String? = null
)

@Serializable
data class QuizScoreDto(
    val id: String,
    val quizId: String,
    val percent: Int = 0,
    val passedAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
    val pairId: String? = null
)

/** Урок из общего расписания пары. */
@Serializable
data class LessonDto(
    val id: String,
    val subject: String = "",
    val weekday: Int = 1,
    val startMinute: Int = 0,
    val endMinute: Int = 0,
    val room: String = "",
    val teacher: String = "",
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
    val pairId: String? = null
)

/** Ответ `POST /v1/pairs` и `POST /v1/pairs/join`. */
@Serializable
data class PairCredentialsDto(
    val pairId: String,
    val deviceToken: String,
    val inviteCode: String = "",
    val serverTime: Long = 0L
)

/** Тело `POST /v1/pairs/join`. */
@Serializable
data class JoinRequestDto(
    val inviteCode: String
)

/** Тело `POST /v1/pairs`; сервер вправе проигнорировать. */
@Serializable
data class CreatePairRequestDto(
    val nameA: String = "",
    val nameB: String = "",
    val sinceMillis: Long = 0L,
    val birthdayA: Long? = null,
    val birthdayB: Long? = null,
    val deviceName: String = "android"
)

/** Обёртка тела `POST /v1/sync`, обязательная серверным API. */
@Serializable
data class PushRequestDto(
    val baseTime: Long = 0L,
    val changes: ChangesDto
)

/** Ответ сервера на `POST /v1/sync`. */
@Serializable
data class PushResponseDto(
    val serverTime: Long = 0L,
    val applied: Int = 0,
    val conflicts: List<String> = emptyList()
)

/** Тело ошибки контракта: `{"error": "unauthorized"}`. */
@Serializable
data class ApiErrorDto(
    val error: String = ""
)