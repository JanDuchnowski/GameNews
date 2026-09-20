package com.example.gamenews.di

import com.example.gamenews.BuildConfig
import com.example.gamenews.data.remote.FreeToGameApi
import com.example.gamenews.data.remote.interceptor.LoggingInterceptorFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://www.freetogame.com/api/"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        // The API adds fields over time; unknown ones must not break parsing.
        ignoreUnknownKeys = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(LoggingInterceptorFactory.create(BuildConfig.DEBUG))
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideFreeToGameApi(retrofit: Retrofit): FreeToGameApi =
        retrofit.create(FreeToGameApi::class.java)
}
