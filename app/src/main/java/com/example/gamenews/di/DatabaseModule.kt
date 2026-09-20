package com.example.gamenews.di

import android.content.Context
import androidx.room.Room
import com.example.gamenews.data.local.GameNewsDatabase
import com.example.gamenews.data.local.dao.GameDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GameNewsDatabase =
        Room.databaseBuilder(context, GameNewsDatabase::class.java, GameNewsDatabase.NAME)
            .build()

    @Provides
    fun provideGameDao(database: GameNewsDatabase): GameDao = database.gameDao()
}
