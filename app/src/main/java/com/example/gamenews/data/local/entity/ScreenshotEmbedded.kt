package com.example.gamenews.data.local.entity

import kotlinx.serialization.Serializable

/**
 * Stored as a JSON column rather than a child table: screenshots are only ever read
 * together with their game and are never queried on their own.
 */
@Serializable
data class ScreenshotEmbedded(
    val id: Int,
    val imageUrl: String,
)
