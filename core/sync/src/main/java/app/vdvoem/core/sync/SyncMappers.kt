package app.vdvoem.core.sync

import app.vdvoem.core.domain.cycle.DEFAULT_CYCLE_LENGTH
import app.vdvoem.core.domain.cycle.DEFAULT_PERIOD_LENGTH
import app.vdvoem.core.domain.model.CalendarEvent
import app.vdvoem.core.domain.model.CycleDay
import app.vdvoem.core.domain.model.CycleSettings
import app.vdvoem.core.domain.model.DiaryEntry
import app.vdvoem.core.domain.model.DiaryMood
import app.vdvoem.core.domain.model.DiaryReminder
import app.vdvoem.core.domain.model.Flow
import app.vdvoem.core.domain.model.Gift
import app.vdvoem.core.domain.model.GiftStatus
import app.vdvoem.core.domain.model.Lesson
import app.vdvoem.core.domain.model.Pair
import app.vdvoem.core.domain.model.Poke
import app.vdvoem.core.domain.model.QuizScore
import app.vdvoem.core.domain.model.SharedTask
import app.vdvoem.core.domain.model.Wish
import app.vdvoem.core.domain.model.WishStatus

/**
 * Единственное место, где домен превращается в DTO и обратно.
 * Новые синхронизируемые сущности добавляются здесь — движок и API не меняются.
 */
object SyncMappers {

    fun Pair.toDto(): PairDto = PairDto(
        id = id,
        nameA = nameA,
        nameB = nameB,
        sinceMillis = sinceMillis,
        birthdayA = birthdayA,
        birthdayB = birthdayB,
        updatedAt = updatedAt
    )

    fun PairDto.toDomain(): Pair = Pair(
        id = id,
        nameA = nameA,
        nameB = nameB,
        sinceMillis = sinceMillis,
        birthdayA = birthdayA,
        birthdayB = birthdayB,
        updatedAt = updatedAt
    )

    fun CalendarEvent.toDto(): EventDto = EventDto(
        id = id,
        title = title,
        dateMillis = dateMillis,
        annual = annual,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        pairId = pairId
    )

    fun EventDto.toDomain(fallbackPairId: String = ""): CalendarEvent = CalendarEvent(
        id = id,
        pairId = pairId?.takeIf { it.isNotBlank() } ?: fallbackPairId,
        title = title,
        dateMillis = dateMillis,
        annual = annual,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

    fun Wish.toDto(): WishDto = WishDto(
        id = id,
        title = title,
        status = status.name,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        pairId = pairId,
        note = note,
        priceEstimate = priceEstimate,
        priority = priority,
        url = url,
        surprise = surprise,
        ownerDeviceId = ownerDeviceId
    )

    fun WishDto.toDomain(fallbackPairId: String = ""): Wish = Wish(
        id = id,
        pairId = pairId?.takeIf { it.isNotBlank() } ?: fallbackPairId,
        title = title,
        note = note,
        priceEstimate = priceEstimate,
        status = runCatching { WishStatus.valueOf(status) }.getOrDefault(WishStatus.WANTED),
        updatedAt = updatedAt,
        priority = priority,
        url = url,
        surprise = surprise,
        ownerDeviceId = ownerDeviceId,
        deletedAt = deletedAt
    )

    fun Gift.toDto(): GiftDto = GiftDto(
        id = id,
        title = title,
        status = status.name,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        pairId = pairId,
        note = note,
        budget = budget,
        wishId = wishId,
        surprise = surprise,
        ownerDeviceId = ownerDeviceId,
        boughtAt = boughtAt,
        givenAt = givenAt
    )

    fun GiftDto.toDomain(fallbackPairId: String = ""): Gift = Gift(
        id = id,
        pairId = pairId?.takeIf { it.isNotBlank() } ?: fallbackPairId,
        title = title,
        note = note,
        budget = budget,
        status = runCatching { GiftStatus.valueOf(status) }.getOrDefault(GiftStatus.IDEA),
        updatedAt = updatedAt,
        wishId = wishId,
        deletedAt = deletedAt,
        surprise = surprise,
        ownerDeviceId = ownerDeviceId,
        boughtAt = boughtAt,
        givenAt = givenAt
    )

    fun Lesson.toDto(): LessonDto = LessonDto(
        id = id,
        subject = subject,
        weekday = weekday.coerceIn(1, 7),
        startMinute = startMinute,
        endMinute = endMinute,
        room = room,
        teacher = teacher,
        textbookBy = textbookBy,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        pairId = pairId
    )

    fun LessonDto.toDomain(fallbackPairId: String = ""): Lesson = Lesson(
        id = id,
        pairId = pairId?.takeIf { it.isNotBlank() } ?: fallbackPairId,
        subject = subject,
        weekday = weekday.coerceIn(1, 7),
        startMinute = startMinute.coerceIn(0, 24 * 60),
        endMinute = endMinute.coerceIn(0, 24 * 60),
        room = room,
        teacher = teacher,
        textbookBy = textbookBy,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

    fun SharedTask.toDto(): TaskDto = TaskDto(
        id = id,
        title = title,
        note = note,
        dueAt = dueAt,
        isCompleted = isCompleted,
        createdBy = createdBy,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        version = version,
        pairId = pairId
    )

    fun TaskDto.toDomain(fallbackPairId: String = ""): SharedTask = SharedTask(
        id = id,
        pairId = pairId?.takeIf { it.isNotBlank() } ?: fallbackPairId,
        title = title,
        note = note,
        dueAt = dueAt,
        isCompleted = isCompleted,
        createdBy = createdBy,
        createdAt = if (createdAt > 0) createdAt else updatedAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        version = version.coerceAtLeast(1)
    )

    fun Poke.toDto(): PokeDto = PokeDto(
        id = id,
        text = text,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        pairId = pairId
    )

    fun PokeDto.toDomain(fallbackPairId: String = ""): Poke = Poke(
        id = id,
        pairId = pairId?.takeIf { it.isNotBlank() } ?: fallbackPairId,
        text = text,
        createdAt = if (createdAt > 0) createdAt else updatedAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

    fun QuizScore.toDto(): QuizScoreDto = QuizScoreDto(
        id = id,
        quizId = quizId,
        percent = percent,
        passedAt = passedAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        pairId = pairId
    )

    fun QuizScoreDto.toDomain(fallbackPairId: String = ""): QuizScore = QuizScore(
        id = id,
        pairId = pairId?.takeIf { it.isNotBlank() } ?: fallbackPairId,
        quizId = quizId,
        percent = percent.coerceIn(0, 100),
        passedAt = if (passedAt > 0) passedAt else updatedAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

    fun CycleDay.toDto(): CycleDayDto = CycleDayDto(
        id = id,
        dateMillis = dateMillis,
        period = period,
        flow = flow.name,
        symptoms = symptoms,
        mood = mood,
        weight = weight,
        note = note,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        pairId = pairId
    )

    fun CycleDayDto.toDomain(fallbackPairId: String = ""): CycleDay = CycleDay(
        id = id,
        pairId = pairId?.takeIf { it.isNotBlank() } ?: fallbackPairId,
        dateMillis = dateMillis,
        period = period,
        flow = runCatching { Flow.valueOf(flow) }.getOrDefault(Flow.NONE),
        symptoms = symptoms.filter { it.isNotBlank() },
        mood = mood,
        weight = weight,
        note = note,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

    fun CycleSettings.toDto(): CycleSettingsDto = CycleSettingsDto(
        id = id,
        cycleLength = cycleLength,
        periodLength = periodLength,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        pairId = pairId
    )

    fun CycleSettingsDto.toDomain(fallbackPairId: String = ""): CycleSettings = CycleSettings(
        id = id,
        pairId = pairId?.takeIf { it.isNotBlank() } ?: fallbackPairId,
        cycleLength = if (cycleLength > 0) cycleLength else DEFAULT_CYCLE_LENGTH,
        periodLength = if (periodLength > 0) periodLength else DEFAULT_PERIOD_LENGTH,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

    fun DiaryEntry.toDto(): DiaryDto = DiaryDto(
        id = id,
        dateMillis = dateMillis,
        mood = mood.name,
        title = title,
        body = body,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        pairId = pairId
    )

    fun DiaryDto.toDomain(fallbackPairId: String = ""): DiaryEntry = DiaryEntry(
        id = id,
        pairId = pairId?.takeIf { it.isNotBlank() } ?: fallbackPairId,
        dateMillis = dateMillis,
        mood = runCatching { DiaryMood.valueOf(mood) }.getOrDefault(DiaryMood.CALM),
        title = title,
        body = body,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )

    fun DiaryReminder.toDto(): DiaryReminderDto = DiaryReminderDto(
        id = id,
        text = text,
        remindAt = remindAt,
        doneAt = doneAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        pairId = pairId
    )

    /** С сервера приходят только общие напоминания: `isPrivate` всегда false. */
    fun DiaryReminderDto.toDomain(fallbackPairId: String = ""): DiaryReminder = DiaryReminder(
        id = id,
        pairId = pairId?.takeIf { it.isNotBlank() } ?: fallbackPairId,
        text = text,
        remindAt = remindAt,
        isPrivate = false,
        doneAt = doneAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt
    )
}