package com.example.gamenews.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Row backing the list screen. Mirrors what `GET /api/games` can supply. */
@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val thumbnailUrl: String,
    val shortDescription: String,
    val genre: String,
    val platform: String,
    val publisher: String,
    val developer: String,
    val releaseDate: String,
)
