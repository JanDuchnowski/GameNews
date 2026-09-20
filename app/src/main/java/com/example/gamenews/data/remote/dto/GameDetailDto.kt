package com.example.gamenews.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wire format of `GET /api/game?id={id}`. */
@Serializable
data class GameDetailDto(
    @SerialName("id") val id: Int,
    @SerialName("title") val title: String? = null,
    @SerialName("thumbnail") val thumbnail: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("short_description") val shortDescription: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("game_url") val gameUrl: String? = null,
    @SerialName("genre") val genre: String? = null,
    @SerialName("platform") val platform: String? = null,
    @SerialName("publisher") val publisher: String? = null,
    @SerialName("developer") val developer: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("freetogame_profile_url") val profileUrl: String? = null,
    @SerialName("minimum_system_requirements") val minimumSystemRequirements: MinimumSystemRequirementsDto? = null,
    @SerialName("screenshots") val screenshots: List<ScreenshotDto> = emptyList(),
)
