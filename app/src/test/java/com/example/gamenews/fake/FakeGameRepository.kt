package com.example.gamenews.fake

import com.example.gamenews.domain.model.Game
import com.example.gamenews.domain.model.GameDetails
import com.example.gamenews.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Lets the ViewModel tests drive state without going through Room or Retrofit. */
class FakeGameRepository : GameRepository {

    val games = MutableStateFlow<List<Game>>(emptyList())
    val details = MutableStateFlow<GameDetails?>(null)

    var refreshGamesResult: Result<Int> = Result.success(0)
    var refreshDetailsResult: Result<Unit> = Result.success(Unit)
    var refreshGamesCallCount: Int = 0

    override fun observeGames(): Flow<List<Game>> = games

    override fun observeGameDetails(gameId: Int): Flow<GameDetails?> = details

    override suspend fun refreshGames(): Result<Int> {
        refreshGamesCallCount++
        return refreshGamesResult
    }

    override suspend fun refreshGameDetails(gameId: Int): Result<Unit> = refreshDetailsResult
}
