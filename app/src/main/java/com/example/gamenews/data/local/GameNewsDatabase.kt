package com.example.gamenews.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.gamenews.data.local.dao.GameDao
import com.example.gamenews.data.local.entity.GameDetailEntity
import com.example.gamenews.data.local.entity.GameEntity

@Database(
    entities = [GameEntity::class, GameDetailEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class GameNewsDatabase : RoomDatabase() {

    abstract fun gameDao(): GameDao

    companion object {
        const val NAME = "gamenews.db"
    }
}
