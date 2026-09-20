package com.example.gamenews.data.remote

import com.example.gamenews.data.remote.dto.GameDetailDto
import com.example.gamenews.data.remote.dto.GameListItemDto
import retrofit2.http.GET
import retrofit2.http.Query

/** FreeToGame REST API. No authentication, no API key. */
interface FreeToGameApi {

    @GET("games")
    suspend fun getGames(): List<GameListItemDto>

    @GET("game")
    suspend fun getGameDetails(@Query("id") id: Int): GameDetailDto
}
