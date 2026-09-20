package com.example.gamenews.ui.list

import com.example.gamenews.domain.model.Game
import com.example.gamenews.ui.UiErrorCause

/**
 * The four states the list screen can be in. Modelled as a sealed interface so the
 * composable has to handle each one and impossible combinations cannot be represented.
 */
sealed interface GameListUiState {

    data object Loading : GameListUiState

    /** Cache is empty and the last refresh succeeded — the API genuinely returned nothing. */
    data object Empty : GameListUiState

    data class Content(
        val games: List<Game>,
        val isRefreshing: Boolean = false,
    ) : GameListUiState

    /** Cache is empty and the last refresh failed. With cached data we show [Content] instead. */
    data class Error(val cause: UiErrorCause) : GameListUiState
}
