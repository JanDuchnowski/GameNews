package com.example.gamenews.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gamenews.domain.repository.GameRepository
import com.example.gamenews.ui.UiErrorCause
import com.example.gamenews.ui.toUiErrorCause
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class GameListViewModel @Inject constructor(
    private val repository: GameRepository,
) : ViewModel() {

    private sealed interface RefreshState {
        data object InFlight : RefreshState

        /** [gameCount] is what the API listed, which the database may not have announced yet. */
        data class Succeeded(val gameCount: Int) : RefreshState
        data class Failed(val cause: UiErrorCause) : RefreshState
    }

    private val refreshState = MutableStateFlow<RefreshState>(RefreshState.InFlight)

    val uiState: StateFlow<GameListUiState> =
        combine(repository.observeGames(), refreshState) { games, refresh ->
            when {
                games.isNotEmpty() -> GameListUiState.Content(
                    games = games,
                    isRefreshing = refresh is RefreshState.InFlight,
                )
                refresh is RefreshState.InFlight -> GameListUiState.Loading
                refresh is RefreshState.Failed -> GameListUiState.Error(refresh.cause)
                // Rows were written; Room has simply not delivered them yet.
                refresh is RefreshState.Succeeded && refresh.gameCount > 0 -> GameListUiState.Loading
                else -> GameListUiState.Empty
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = GameListUiState.Loading,
        )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            refreshState.value = RefreshState.InFlight
            refreshState.value = repository.refreshGames().fold(
                onSuccess = { gameCount -> RefreshState.Succeeded(gameCount) },
                onFailure = { RefreshState.Failed(it.toUiErrorCause()) },
            )
        }
    }
}

/** Keeps the upstream Flows alive across a configuration change without leaking them. */
private const val STOP_TIMEOUT_MILLIS = 5_000L
