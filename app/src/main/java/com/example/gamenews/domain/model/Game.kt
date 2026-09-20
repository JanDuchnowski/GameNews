package com.example.gamenews.domain.model

/**
 * A game as returned by the list endpoint. Deliberately smaller than [GameDetails]:
 * the list endpoint simply does not return the richer fields.
 */
data class Game(
    val id: Int,
    val title: String,
    val thumbnailUrl: String,
    val shortDescription: String,
    val genre: String,
    val platform: String,
    val publisher: String,
    val developer: String,
    val releaseDate: String,
)
