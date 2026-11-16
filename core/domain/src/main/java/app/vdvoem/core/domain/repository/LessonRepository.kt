package app.vdvoem.core.domain.repository

import app.vdvoem.core.domain.model.Lesson
import kotlinx.coroutines.flow.Flow

/** Расписание уроков: local-first, всё через этот репозиторий (нужно для синка). */
interface LessonRepository {
    fun observe(): Flow<List<Lesson>>

    suspend fun save(lesson: Lesson)

    suspend fun delete(lesson: Lesson)
}