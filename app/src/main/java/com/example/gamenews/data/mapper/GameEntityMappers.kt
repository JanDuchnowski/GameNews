package com.example.gamenews.data.mapper

import com.example.gamenews.data.local.entity.GameDetailEntity
import com.example.gamenews.data.local.entity.GameEntity
import com.example.gamenews.data.local.entity.SystemRequirementsEmbedded
import com.example.gamenews.domain.model.Game
import com.example.gamenews.domain.model.GameDetails
import com.example.gamenews.domain.model.Screenshot
import com.example.gamenews.domain.model.SystemRequirements

/** Entity -> domain model. Keeps Room types out of the domain and UI layers. */
fun GameEntity.toDomain(): Game = Game(
    id = id,
    title = title,
    thumbnailUrl = thumbnailUrl,
    shortDescription = shortDescription,
    genre = genre,
    platform = platform,
    publisher = publisher,
    developer = developer,
    releaseDate = releaseDate,
)

fun GameDetailEntity.toDomain(): GameDetails = GameDetails(
    id = id,
    title = title,
    thumbnailUrl = thumbnailUrl,
    status = status,
    shortDescription = shortDescription,
    description = description,
    gameUrl = gameUrl,
    genre = genre,
    platform = platform,
    publisher = publisher,
    developer = developer,
    releaseDate = releaseDate,
    minimumSystemRequirements = minimumSystemRequirements?.toDomain(),
    screenshots = screenshots.map { Screenshot(id = it.id, imageUrl = it.imageUrl) },
)

fun List<GameEntity>.toDomain(): List<Game> = map { it.toDomain() }

private fun SystemRequirementsEmbedded.toDomain(): SystemRequirements = SystemRequirements(
    os = os,
    processor = processor,
    memory = memory,
    graphics = graphics,
    storage = storage,
)
