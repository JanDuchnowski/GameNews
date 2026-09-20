package com.example.gamenews.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wire format of one element of `GET /api/games`. */
@Serializable
data class GameListItemDto(
    @SerialName("id") val id: Int,
    @SerialName("title") val title: String? = null,
    @SerialName("thumbnail") val thumbnail: String? = null,
    @SerialName("short_description") val shortDescription: String? = null,
    @SerialName("game_url") val gameUrl: String? = null,
    @SerialName("genre") val genre: String? = null,
    @SerialName("platform") val platform: String? = null,
    @SerialName("publisher") val publisher: String? = null,
    @SerialName("developer") val developer: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("freetogame_profile_url") val profileUrl: String? = null,
)
