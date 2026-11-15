package app.vdvoem.core.data.repository

import app.vdvoem.core.data.db.PokeDao
import app.vdvoem.core.data.db.PokeEntity
import app.vdvoem.core.domain.model.Poke
import app.vdvoem.core.domain.repository.PokeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomPokeRepository(
    private val dao: PokeDao
) : PokeRepository {

    override fun observe(): Flow<List<Poke>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun save(poke: Poke) {
        dao.upsert(poke.toEntity())
    }
}

private fun PokeEntity.toDomain() = Poke(
    id = id,
    pairId = pairId,
    text = text,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

private fun Poke.toEntity() = PokeEntity(
    id = id,
    pairId = pairId,
    text = text,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)