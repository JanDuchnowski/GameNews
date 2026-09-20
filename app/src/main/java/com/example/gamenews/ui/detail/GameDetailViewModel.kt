package com.example.gamenews.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.gamenews.domain.repository.GameNotFoundException
import com.example.gamenews.domain.repository.GameRepository
import com.example.gamenews.ui.GameDetailDestination
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
class GameDetailViewModel @Inject constructor(
    private val repository: GameRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private sealed interface RefreshState {
        data object Succeeded : RefreshState
        data object InFlight : RefreshState

        /** The API answered that this game is gone; retrying would not help. */
        data object NotFound : RefreshState
        data class Failed(val cause: UiErrorCause) : RefreshState
    }

    private val gameId: Int = savedStateHandle.toRoute<GameDetailDestination>().gameId

    private val refreshState = MutableStateFlow<RefreshState>(RefreshState.InFlight)

    val uiState: StateFlow<GameDetailUiState> =
        combine(repository.observeGameDetails(gameId), refreshState) { details, refresh ->
            when {
                details != null -> GameDetailUiState.Content(
                    details = details,
                    isRefreshing = refresh is RefreshState.InFlight,
                )
                refresh is RefreshState.InFlight -> GameDetailUiState.Loading
                refresh is RefreshState.NotFound -> GameDetailUiState.Empty
                refresh is RefreshState.Failed -> GameDetailUiState.Error(refresh.cause)
                // Succeeded means a row was written; Room has not delivered it yet.
                else -> GameDetailUiState.Loading
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = GameDetailUiState.Loading,
        )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            refreshState.value = RefreshState.InFlight
            refreshState.value = repository.refreshGameDetails(gameId).fold(
                onSuccess = { RefreshState.Succeeded },
                onFailure = { cause ->
                    if (cause is GameNotFoundException) {
                        RefreshState.NotFound
                    } else {
                        RefreshState.Failed(cause.toUiErrorCause())
                    }
                },
            )
        }
    }
}

/** Keeps the upstream Flows alive across a configuration change without leaking them. */
private const val STOP_TIMEOUT_MILLIS = 5_000L
