package com.example.gamenews.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScreenshotDto(
    @SerialName("id") val id: Int,
    @SerialName("image") val image: String? = null,
)
