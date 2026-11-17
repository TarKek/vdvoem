package app.vdvoem.core.data.repository

import app.vdvoem.core.data.LocalChangeSignal
import app.vdvoem.core.data.db.PairDao
import app.vdvoem.core.data.db.PairEntity
import app.vdvoem.core.domain.model.Pair
import app.vdvoem.core.domain.repository.PairRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomPairRepository(
    private val dao: PairDao
) : PairRepository {

    override fun observe(): Flow<Pair?> = dao.observe().map { it?.toDomain() }

    override suspend fun save(pair: Pair) {
        dao.replace(pair.toEntity())
        LocalChangeSignal.notifyLocalWrite()
    }

    override suspend fun current(): Pair? = dao.current()?.toDomain()
}

private fun PairEntity.toDomain() = Pair(
    id = id,
    nameA = nameA,
    nameB = nameB,
    sinceMillis = sinceMillis,
    birthdayA = birthdayA,
    birthdayB = birthdayB,
    updatedAt = updatedAt
)

private fun Pair.toEntity() = PairEntity(
    id = id,
    pairId = id,
    nameA = nameA,
    nameB = nameB,
    sinceMillis = sinceMillis,
    birthdayA = birthdayA,
    birthdayB = birthdayB,
    updatedAt = updatedAt
)
