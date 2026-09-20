package com.example.gamenews.ui.detail

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.example.gamenews.domain.model.GameDetails
import com.example.gamenews.domain.repository.GameNotFoundException
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric, unlike the other ViewModel tests: reading the type-safe navigation
 * argument goes through SavedStateHandle -> Bundle, and a bare JVM test has no Bundle.
 * The plain Application keeps Hilt out of it, since the ViewModel is constructed directly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class GameDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeGameRepository()

    @Test
    fun `emits Loading before the first refresh completes`() = runTest {
        val viewModel = createViewModel()

        assertEquals(GameDetailUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `emits Content once the repository emits details`() = runTest {
        repository.details.value = details

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(GameDetailUiState.Content(details = details), viewModel.uiState.value)
    }

    /**
     * A 404 is not a transport failure: the game is gone, so retrying cannot help and the
     * screen shows an empty state rather than an error with a retry button.
     */
    @Test
    fun `emits Empty when the refresh reports the game does not exist`() = runTest {
        repository.refreshDetailsResult = Result.failure(GameNotFoundException(gameId = GAME_ID))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(GameDetailUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `emits Error when the refresh fails and there is nothing cached`() = runTest {
        repository.refreshDetailsResult = Result.failure(IOException("offline"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(GameDetailUiState.Error(UiErrorCause.NETWORK), viewModel.uiState.value)
    }

    /** A successful refresh wrote a row, so the screen waits for it instead of showing Empty. */
    @Test
    fun `stays Loading when the refresh succeeded and the cache has not delivered yet`() = runTest {
        repository.refreshDetailsResult = Result.success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(GameDetailUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `keeps showing cached details when the refresh fails`() = runTest {
        repository.details.value = details
        repository.refreshDetailsResult = Result.failure(IOException("offline"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(GameDetailUiState.Content(details = details), viewModel.uiState.value)
    }

    private fun TestScope.createViewModel(): GameDetailViewModel =
        GameDetailViewModel(
            repository = repository,
            savedStateHandle = SavedStateHandle(mapOf("gameId" to GAME_ID)),
        ).also { viewModel ->
            backgroundScope.launch { viewModel.uiState.collect() }
        }

    private val details = GameDetails(
        id = GAME_ID,
        title = "Warframe",
        thumbnailUrl = "",
        status = "Live",
        shortDescription = "",
        description = "",
        gameUrl = "",
        genre = "MMORPG",
        platform = "PC (Windows)",
        publisher = "Digital Extremes",
        developer = "Digital Extremes",
        releaseDate = "2013-03-25",
        minimumSystemRequirements = null,
        screenshots = emptyList(),
    )

    private companion object {
        const val GAME_ID = 7
    }
}
