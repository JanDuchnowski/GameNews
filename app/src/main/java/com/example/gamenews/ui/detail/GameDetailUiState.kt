package com.example.gamenews.ui.detail

import com.example.gamenews.domain.model.GameDetails
import com.example.gamenews.ui.UiErrorCause

sealed interface GameDetailUiState {

    data object Loading : GameDetailUiState

    /** No cached detail row and the refresh reported the game does not exist. */
    data object Empty : GameDetailUiState

    data class Content(
        val details: GameDetails,
        val isRefreshing: Boolean = false,
    ) : GameDetailUiState

    data class Error(val cause: UiErrorCause) : GameDetailUiState
}
