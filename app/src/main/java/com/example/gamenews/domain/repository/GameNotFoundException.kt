package com.example.gamenews.domain.repository

/**
 * The API answered, and the answer was that this game does not exist. Distinct from a
 * transport failure: retrying will not help, so the UI shows an empty state, not an error.
 */
class GameNotFoundException(val gameId: Int) : Exception("No game with id $gameId")
