package com.example.gamenews.domain.model

/** A single game as returned by the detail endpoint. */
data class GameDetails(
    val id: Int,
    val title: String,
    val thumbnailUrl: String,
    val status: String,
    val shortDescription: String,
    val description: String,
    val gameUrl: String,
    val genre: String,
    val platform: String,
    val publisher: String,
    val developer: String,
    val releaseDate: String,
    val minimumSystemRequirements: SystemRequirements?,
    val screenshots: List<Screenshot>,
)
