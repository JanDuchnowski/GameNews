package com.example.gamenews.data.mapper

import com.example.gamenews.data.local.entity.GameEntity
import com.example.gamenews.data.remote.dto.GameDetailDto
import com.example.gamenews.data.remote.dto.GameListItemDto
import com.example.gamenews.data.remote.dto.MinimumSystemRequirementsDto
import com.example.gamenews.data.remote.dto.ScreenshotDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameMapperTest {

    @Test
    fun `list dto maps every field onto the entity`() {
        val dto = GameListItemDto(
            id = 452,
            title = "Call of Duty: Warzone",
            thumbnail = "https://example.com/thumb.jpg",
            shortDescription = "A free-to-play battle royale.",
            gameUrl = "https://example.com/open",
            genre = "Shooter",
            platform = "PC (Windows)",
            publisher = "Activision",
            developer = "Infinity Ward",
            releaseDate = "2020-03-10",
            profileUrl = "https://example.com/profile",
        )

        val entity = dto.toEntity()

        assertEquals(452, entity.id)
        assertEquals("Call of Duty: Warzone", entity.title)
        assertEquals("https://example.com/thumb.jpg", entity.thumbnailUrl)
        assertEquals("A free-to-play battle royale.", entity.shortDescription)
        assertEquals("Shooter", entity.genre)
        assertEquals("PC (Windows)", entity.platform)
        assertEquals("Activision", entity.publisher)
        assertEquals("Infinity Ward", entity.developer)
        assertEquals("2020-03-10", entity.releaseDate)
    }

    @Test
    fun `list dto with null fields maps to empty strings`() {
        val entity = GameListItemDto(id = 1).toEntity()

        assertEquals(1, entity.id)
        assertEquals("", entity.title)
        assertEquals("", entity.thumbnailUrl)
        assertEquals("", entity.shortDescription)
        assertEquals("", entity.genre)
        assertEquals("", entity.platform)
        assertEquals("", entity.publisher)
        assertEquals("", entity.developer)
        assertEquals("", entity.releaseDate)
    }

    @Test
    fun `detail dto maps requirements and screenshots`() {
        val dto = GameDetailDto(
            id = 452,
            title = "Warframe",
            description = "A long description.",
            status = "Live",
            minimumSystemRequirements = MinimumSystemRequirementsDto(
                os = "Windows 10",
                processor = "Intel i5",
                memory = "8 GB",
                graphics = "GTX 1050",
                storage = "50 GB",
            ),
            screenshots = listOf(
                ScreenshotDto(id = 1, image = "https://example.com/1.jpg"),
                ScreenshotDto(id = 2, image = null),
            ),
        )

        val entity = dto.toEntity()

        assertEquals("Warframe", entity.title)
        assertEquals("A long description.", entity.description)
        assertEquals("Live", entity.status)
        assertEquals("Windows 10", entity.minimumSystemRequirements?.os)
        assertEquals("GTX 1050", entity.minimumSystemRequirements?.graphics)
        assertEquals(2, entity.screenshots.size)
        assertEquals("https://example.com/1.jpg", entity.screenshots[0].imageUrl)
        // A screenshot row with no URL still maps, with an empty string rather than null.
        assertEquals("", entity.screenshots[1].imageUrl)
    }

    @Test
    fun `detail dto without requirements maps to a null embedded object`() {
        val entity = GameDetailDto(id = 7, minimumSystemRequirements = null).toEntity()

        assertNull(entity.minimumSystemRequirements)
        assertEquals(emptyList<Any>(), entity.screenshots)
    }

    @Test
    fun `entity maps onto the domain model`() {
        val entity = GameEntity(
            id = 3,
            title = "Destiny 2",
            thumbnailUrl = "https://example.com/d2.jpg",
            shortDescription = "A looter shooter.",
            genre = "Shooter",
            platform = "PC (Windows)",
            publisher = "Bungie",
            developer = "Bungie",
            releaseDate = "2017-09-06",
        )

        val game = entity.toDomain()

        assertEquals(3, game.id)
        assertEquals("Destiny 2", game.title)
        assertEquals("https://example.com/d2.jpg", game.thumbnailUrl)
        assertEquals("A looter shooter.", game.shortDescription)
        assertEquals("Shooter", game.genre)
        assertEquals("PC (Windows)", game.platform)
        assertEquals("Bungie", game.publisher)
        assertEquals("Bungie", game.developer)
        assertEquals("2017-09-06", game.releaseDate)
    }
}
