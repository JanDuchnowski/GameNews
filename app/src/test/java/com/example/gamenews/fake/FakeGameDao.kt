package com.example.gamenews.fake

import com.example.gamenews.data.local.dao.GameDao
import com.example.gamenews.data.local.entity.GameDetailEntity
import com.example.gamenews.data.local.entity.GameEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory stand-in for Room. Backed by MutableStateFlows so a write is observable by
 * the Flows the repository returns, which is the behaviour the real DAO provides.
 */
class FakeGameDao : GameDao {

    val games = MutableStateFlow<List<GameEntity>>(emptyList())
    val details = MutableStateFlow<Map<Int, GameDetailEntity>>(emptyMap())

    override fun observeGames(): Flow<List<GameEntity>> =
        games.map { rows -> rows.sortedBy(GameEntity::title) }

    override fun observeGameDetails(gameId: Int): Flow<GameDetailEntity?> =
        details.map { rows -> rows[gameId] }

    override suspend fun upsertGames(games: List<GameEntity>) {
        this.games.update { current ->
            (current.associateBy(GameEntity::id) + games.associateBy(GameEntity::id)).values.toList()
        }
    }

    override suspend fun upsertGameDetails(details: GameDetailEntity) {
        this.details.update { current -> current + (details.id to details) }
    }

    override suspend fun deleteGamesNotIn(keptIds: List<Int>) {
        games.update { current -> current.filter { it.id in keptIds } }
    }
}
