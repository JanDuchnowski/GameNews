package com.example.gamenews.data.repository

import app.cash.turbine.test
import com.example.gamenews.data.local.entity.GameEntity
import com.example.gamenews.data.remote.dto.GameDetailDto
import com.example.gamenews.data.remote.dto.GameListItemDto
import com.example.gamenews.fake.FakeGameApi
import com.example.gamenews.fake.FakeGameDao
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRepositoryImplTest {

    private val api = FakeGameApi()
    private val dao = FakeGameDao()
    private val repository = GameRepositoryImpl(api = api, dao = dao)

    @Test
    fun `observeGames reads from the dao and never calls the api`() = runTest {
        repository.observeGames().test {
            assertEquals(emptyList<Any>(), awaitItem())

            dao.upsertGames(listOf(entity(id = 1, title = "Warframe")))

            val games = awaitItem()
            assertEquals(1, games.size)
            assertEquals("Warframe", games.single().title)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(0, api.getGamesCallCount)
    }

    @Test
    fun `refreshGames writes the api response into the dao`() = runTest {
        api.nextGamesResult = Result.success(
            listOf(
                GameListItemDto(id = 1, title = "Warframe"),
                GameListItemDto(id = 2, title = "Destiny 2"),
            ),
        )

        val result = repository.refreshGames()

        assertEquals(2, result.getOrNull())
        assertEquals(setOf(1, 2), dao.games.value.map(GameEntity::id).toSet())
        assertEquals(
            listOf("Destiny 2", "Warframe"),
            repository.observeGames().first().map { it.title },
        )
    }

    @Test
    fun `refreshGames removes rows the api no longer returns`() = runTest {
        dao.upsertGames(listOf(entity(id = 99, title = "Delisted Game")))
        api.nextGamesResult = Result.success(listOf(GameListItemDto(id = 1, title = "Warframe")))

        repository.refreshGames()

        assertEquals(listOf(1), dao.games.value.map(GameEntity::id))
    }

    @Test
    fun `refreshGames returns a failure and leaves the cache untouched when the api throws`() = runTest {
        val cached = listOf(entity(id = 1, title = "Warframe"))
        dao.upsertGames(cached)
        api.nextGamesResult = Result.failure(IOException("offline"))

        val result = repository.refreshGames()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals(cached, dao.games.value)
    }

    @Test
    fun `refreshGameDetails writes one detail row`() = runTest {
        api.nextDetailResult = Result.success(
            GameDetailDto(id = 5, title = "Warframe", description = "Ninjas play free."),
        )

        val result = repository.refreshGameDetails(gameId = 5)

        assertTrue(result.isSuccess)
        assertEquals("Ninjas play free.", dao.details.value.getValue(5).description)
        assertEquals("Warframe", repository.observeGameDetails(5).first()?.title)
    }

    private fun entity(id: Int, title: String) = GameEntity(
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
}
