package com.example.gamenews.ui.list

import com.example.gamenews.domain.model.Game
import com.example.gamenews.fake.FakeGameRepository
import com.example.gamenews.ui.UiErrorCause
import com.example.gamenews.util.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeGameRepository()

    @Test
    fun `emits Loading before the first refresh completes`() = runTest {
        val viewModel = createViewModel()

        // The refresh coroutine has not run yet on the StandardTestDispatcher.
        assertEquals(GameListUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `emits Content once the repository emits games`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        repository.games.value = games
        advanceUntilIdle()

        assertEquals(GameListUiState.Content(games = games), viewModel.uiState.value)
    }

    @Test
    fun `emits Empty when the cache stays empty and the refresh succeeds`() = runTest {
        repository.refreshGamesResult = Result.success(0)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(GameListUiState.Empty, viewModel.uiState.value)
    }

    /**
     * Room announces a write asynchronously, so there is a window where the refresh has
     * succeeded and the games Flow has not caught up. Showing Empty there flashes
     * "no games" on every cold start.
     */
    @Test
    fun `stays Loading when the refresh wrote games the cache has not delivered yet`() = runTest {
        repository.refreshGamesResult = Result.success(417)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(GameListUiState.Loading, viewModel.uiState.value)

        repository.games.value = games
        advanceUntilIdle()

        assertEquals(GameListUiState.Content(games = games), viewModel.uiState.value)
    }

    @Test
    fun `emits Error when the refresh fails and there is nothing cached`() = runTest {
        repository.refreshGamesResult = Result.failure(IOException("offline"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(GameListUiState.Error(UiErrorCause.NETWORK), viewModel.uiState.value)
    }

    @Test
    fun `keeps showing cached games when the refresh fails`() = runTest {
        repository.games.value = games
        repository.refreshGamesResult = Result.failure(IOException("offline"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(GameListUiState.Content(games = games), viewModel.uiState.value)
    }

    @Test
    fun `retrying from the Error state triggers another refresh`() = runTest {
        repository.refreshGamesResult = Result.failure(IOException("offline"))
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(GameListUiState.Error(UiErrorCause.NETWORK), viewModel.uiState.value)

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(2, repository.refreshGamesCallCount)
    }

    /**
     * uiState is shared WhileSubscribed, so it only tracks the repository while something
     * collects it. backgroundScope's collector is cancelled when the test ends.
     */
    private fun TestScope.createViewModel(): GameListViewModel =
        GameListViewModel(repository).also { viewModel ->
            backgroundScope.launch { viewModel.uiState.collect() }
        }

    private val games = listOf(
        game(id = 1, title = "Destiny 2"),
        game(id = 2, title = "Warframe"),
    )

    private fun game(id: Int, title: String) = Game(
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
