package com.example.gamenews.data.local

import androidx.room.TypeConverter
import com.example.gamenews.data.local.entity.ScreenshotEmbedded
import kotlinx.serialization.json.Json

class Converters {

    @TypeConverter
    fun screenshotsToJson(screenshots: List<ScreenshotEmbedded>): String =
        json.encodeToString(screenshots)

    @TypeConverter
    fun screenshotsFromJson(value: String): List<ScreenshotEmbedded> =
        json.decodeFromString(value)

    private companion object {
        val json = Json
    }
}
