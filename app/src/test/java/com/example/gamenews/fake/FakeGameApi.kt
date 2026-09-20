package com.example.gamenews.fake

import com.example.gamenews.data.remote.FreeToGameApi
import com.example.gamenews.data.remote.dto.GameDetailDto
import com.example.gamenews.data.remote.dto.GameListItemDto

/**
 * Hand-written stand-in for the network. `nextGamesResult` lets a test make a call fail
 * without a mocking framework and without an HTTP server.
 */
class FakeGameApi : FreeToGameApi {

    var nextGamesResult: Result<List<GameListItemDto>> = Result.success(emptyList())
    var nextDetailResult: Result<GameDetailDto>? = null
    var getGamesCallCount: Int = 0

    override suspend fun getGames(): List<GameListItemDto> {
        getGamesCallCount++
        return nextGamesResult.getOrThrow()
    }

    override suspend fun getGameDetails(id: Int): GameDetailDto =
        checkNotNull(nextDetailResult) { "nextDetailResult was not set by the test" }.getOrThrow()
}
