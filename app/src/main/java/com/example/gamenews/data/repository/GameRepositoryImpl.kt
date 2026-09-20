package com.example.gamenews.data.repository

import com.example.gamenews.data.local.dao.GameDao
import com.example.gamenews.data.mapper.toDomain
import com.example.gamenews.data.mapper.toEntities
import com.example.gamenews.data.mapper.toEntity
import com.example.gamenews.data.remote.FreeToGameApi
import com.example.gamenews.domain.model.Game
import com.example.gamenews.domain.model.GameDetails
import com.example.gamenews.domain.repository.GameNotFoundException
import com.example.gamenews.domain.repository.GameRepository
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException

/**
 * Offline-first: the observe* functions read only from Room, the refresh* functions
 * only write to it. A failed refresh leaves whatever is cached untouched.
 */
@Singleton
class GameRepositoryImpl @Inject constructor(
    private val api: FreeToGameApi,
    private val dao: GameDao,
) : GameRepository {

    override fun observeGames(): Flow<List<Game>> =
        dao.observeGames().map { entities -> entities.toDomain() }

    override fun observeGameDetails(gameId: Int): Flow<GameDetails?> =
        dao.observeGameDetails(gameId).map { entity -> entity?.toDomain() }

    override suspend fun refreshGames(): Result<Int> = runCatchingCancellable {
        val entities = api.getGames().toEntities()
        dao.upsertGames(entities)
        // The API is the authority on which games exist; anything it dropped is stale.
        dao.deleteGamesNotIn(entities.map { it.id })
        entities.size
    }

    override suspend fun refreshGameDetails(gameId: Int): Result<Unit> = runCatchingCancellable {
        dao.upsertGameDetails(api.getGameDetails(gameId).toEntity())
    }.recoverCatching { cause ->
        throw if (cause.isNotFound()) GameNotFoundException(gameId) else cause
    }
}

private fun Throwable.isNotFound(): Boolean =
    this is HttpException && code() == HttpURLConnection.HTTP_NOT_FOUND

/**
 * [runCatching] also swallows [CancellationException], which would turn a cancelled
 * viewModelScope into a spurious failure and break structured concurrency.
 */
private inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        Result.failure(throwable)
    }
