package com.example.gamenews.data.mapper

import com.example.gamenews.data.local.entity.GameDetailEntity
import com.example.gamenews.data.local.entity.GameEntity
import com.example.gamenews.data.local.entity.ScreenshotEmbedded
import com.example.gamenews.data.local.entity.SystemRequirementsEmbedded
import com.example.gamenews.data.remote.dto.GameDetailDto
import com.example.gamenews.data.remote.dto.GameListItemDto
import com.example.gamenews.data.remote.dto.MinimumSystemRequirementsDto

/**
 * DTO -> Entity. This is the layer that decides what a missing field means, so the rest
 * of the app never has to deal with the API's nullable strings.
 */
fun GameListItemDto.toEntity(): GameEntity = GameEntity(
    id = id,
    title = title.orEmpty(),
    thumbnailUrl = thumbnail.orEmpty(),
    shortDescription = shortDescription.orEmpty(),
    genre = genre.orEmpty(),
    platform = platform.orEmpty(),
    publisher = publisher.orEmpty(),
    developer = developer.orEmpty(),
    releaseDate = releaseDate.orEmpty(),
)

fun GameDetailDto.toEntity(): GameDetailEntity = GameDetailEntity(
    id = id,
    title = title.orEmpty(),
    thumbnailUrl = thumbnail.orEmpty(),
    status = status.orEmpty(),
    shortDescription = shortDescription.orEmpty(),
    description = description.orEmpty(),
    gameUrl = gameUrl.orEmpty(),
    genre = genre.orEmpty(),
    platform = platform.orEmpty(),
    publisher = publisher.orEmpty(),
    developer = developer.orEmpty(),
    releaseDate = releaseDate.orEmpty(),
    minimumSystemRequirements = minimumSystemRequirements?.toEmbedded(),
    screenshots = screenshots.map { ScreenshotEmbedded(id = it.id, imageUrl = it.image.orEmpty()) },
)

fun List<GameListItemDto>.toEntities(): List<GameEntity> = map { it.toEntity() }

/**
 * Nulls survive here, unlike the string fields above: the API omits these for non-PC
 * titles, so "absent" is information the detail screen wants rather than noise.
 */
private fun MinimumSystemRequirementsDto.toEmbedded(): SystemRequirementsEmbedded =
    SystemRequirementsEmbedded(
        os = os,
        processor = processor,
        memory = memory,
        graphics = graphics,
        storage = storage,
    )
