package com.example.gamenews.domain.repository

import com.example.gamenews.domain.model.Game
import com.example.gamenews.domain.model.GameDetails
import kotlinx.coroutines.flow.Flow

/**
 * Reads are Flows backed by the database; refreshes are one-shot suspend calls that
 * only write to the database. Callers never receive network payloads directly.
 */
interface GameRepository {

    fun observeGames(): Flow<List<Game>>

    fun observeGameDetails(gameId: Int): Flow<GameDetails?>

    /**
     * Returns how many games the API listed. A caller cannot get this from
     * [observeGames]: Room announces the write asynchronously, so an empty list there
     * means "nothing yet" and not necessarily "nothing at all".
     */
    suspend fun refreshGames(): Result<Int>

    suspend fun refreshGameDetails(gameId: Int): Result<Unit>
}
