package com.example.gamenews.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Separate table from [GameEntity]: detail rows are fetched one at a time and the list
 * endpoint cannot fill these columns, so merging the two would mean half-empty rows.
 */
@Entity(tableName = "game_details")
data class GameDetailEntity(
    @PrimaryKey val id: Int,
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
    @Embedded(prefix = "req_") val minimumSystemRequirements: SystemRequirementsEmbedded?,
    val screenshots: List<ScreenshotEmbedded>,
)
