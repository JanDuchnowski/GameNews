package com.example.gamenews.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MinimumSystemRequirementsDto(
    @SerialName("os") val os: String? = null,
    @SerialName("processor") val processor: String? = null,
    @SerialName("memory") val memory: String? = null,
    @SerialName("graphics") val graphics: String? = null,
    @SerialName("storage") val storage: String? = null,
)
