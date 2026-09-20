package com.example.gamenews.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.example.gamenews.data.local.dao.GameDao
import com.example.gamenews.data.local.entity.GameDetailEntity
import com.example.gamenews.data.local.entity.GameEntity
import com.example.gamenews.data.local.entity.ScreenshotEmbedded
import com.example.gamenews.data.local.entity.SystemRequirementsEmbedded
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs against a real SQLite database. These are the assertions the JVM tests cannot
 * make: that the queries are valid SQL, that the type converters round-trip, and that a
 * write really does push a new value down the read Flow.
 */
@RunWith(AndroidJUnit4::class)
class GameDaoTest {

    private lateinit var database: GameNewsDatabase
    private lateinit var dao: GameDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, GameNewsDatabase::class.java)
            .build()
        dao = database.gameDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeGames_sortsByTitle() = runTest {
        dao.upsertGames(
            listOf(
                game(id = 1, title = "Warframe"),
                game(id = 2, title = "Apex Legends"),
                game(id = 3, title = "Destiny 2"),
            ),
        )

        assertEquals(
            listOf("Apex Legends", "Destiny 2", "Warframe"),
            dao.observeGames().first().map(GameEntity::title),
        )
    }

    /** The property the whole offline-first design rests on: a write feeds the read Flow. */
    @Test
    fun observeGames_emitsAgainAfterAWrite() = runTest {
        dao.observeGames().test {
            assertEquals(emptyList<GameEntity>(), awaitItem())

            dao.upsertGames(listOf(game(id = 1, title = "Warframe")))

            assertEquals(listOf("Warframe"), awaitItem().map(GameEntity::title))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun upsertGames_replacesARowWithTheSameId() = runTest {
        dao.upsertGames(listOf(game(id = 1, title = "Old Title")))
        dao.upsertGames(listOf(game(id = 1, title = "New Title")))

        val games = dao.observeGames().first()
        assertEquals(1, games.size)
        assertEquals("New Title", games.single().title)
    }

    @Test
    fun deleteGamesNotIn_keepsOnlyTheListedIds() = runTest {
        dao.upsertGames(
            listOf(
                game(id = 1, title = "Warframe"),
                game(id = 2, title = "Delisted"),
                game(id = 3, title = "Destiny 2"),
            ),
        )

        dao.deleteGamesNotIn(listOf(1, 3))

        assertEquals(listOf(1, 3), dao.observeGames().first().map(GameEntity::id).sorted())
    }

    /** Covers the JSON type converter and the nullable @Embedded column group. */
    @Test
    fun gameDetails_roundTripsRequirementsAndScreenshots() = runTest {
        val details = GameDetailEntity(
            id = 7,
            title = "Warframe",
            thumbnailUrl = "https://example.com/thumb.jpg",
            status = "Live",
            shortDescription = "Ninjas play free.",
            description = "A long description.",
            gameUrl = "https://example.com/open",
            genre = "MMORPG",
            platform = "PC (Windows)",
            publisher = "Digital Extremes",
            developer = "Digital Extremes",
            releaseDate = "2013-03-25",
            minimumSystemRequirements = SystemRequirementsEmbedded(
                os = "Windows 10",
                processor = "Intel i5",
                memory = "4 GB",
                graphics = "GTX 660",
                storage = "35 GB",
            ),
            screenshots = listOf(
                ScreenshotEmbedded(id = 1, imageUrl = "https://example.com/1.jpg"),
                ScreenshotEmbedded(id = 2, imageUrl = "https://example.com/2.jpg"),
            ),
        )

        dao.upsertGameDetails(details)

        val stored = dao.observeGameDetails(gameId = 7).first()
        assertEquals("Windows 10", stored?.minimumSystemRequirements?.os)
        assertEquals("35 GB", stored?.minimumSystemRequirements?.storage)
        assertEquals(2, stored?.screenshots?.size)
        assertEquals("https://example.com/2.jpg", stored?.screenshots?.get(1)?.imageUrl)
    }

    @Test
    fun gameDetails_storeANullRequirementsGroup() = runTest {
        dao.upsertGameDetails(detailsWithoutRequirements(id = 8))

        assertNull(dao.observeGameDetails(gameId = 8).first()?.minimumSystemRequirements)
    }

    @Test
    fun observeGameDetails_emitsNullForAnAbsentRow() = runTest {
        assertNull(dao.observeGameDetails(gameId = 404).first())
    }

    private fun game(id: Int, title: String) = GameEntity(
        id = id,
        title = title,
        thumbnailUrl = "",
        shortDescription = "",
        genre = "",
        platform = "",
        publisher = "",
        developer = "",
        releaseDate = "",
    )

    private fun detailsWithoutRequirements(id: Int) = GameDetailEntity(
        id = id,
        title = "No Requirements",
        thumbnailUrl = "",
        status = "",
        shortDescription = "",
        description = "",
        gameUrl = "",
        genre = "",
        platform = "",
        publisher = "",
        developer = "",
        releaseDate = "",
        minimumSystemRequirements = null,
        screenshots = emptyList(),
    )
}
