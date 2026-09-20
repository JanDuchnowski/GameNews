package com.example.gamenews.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.gamenews.data.local.entity.GameDetailEntity
import com.example.gamenews.data.local.entity.GameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Query("SELECT * FROM games ORDER BY title ASC")
    fun observeGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM game_details WHERE id = :gameId")
    fun observeGameDetails(gameId: Int): Flow<GameDetailEntity?>

    @Upsert
    suspend fun upsertGames(games: List<GameEntity>)

    @Upsert
    suspend fun upsertGameDetails(details: GameDetailEntity)

    @Query("DELETE FROM games WHERE id NOT IN (:keptIds)")
    suspend fun deleteGamesNotIn(keptIds: List<Int>)
}
