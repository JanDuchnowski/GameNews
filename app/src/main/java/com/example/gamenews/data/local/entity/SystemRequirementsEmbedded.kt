package com.example.gamenews.data.local.entity

/** Flattened into `game_details` with a `req_` column prefix. */
data class SystemRequirementsEmbedded(
    val os: String?,
    val processor: String?,
    val memory: String?,
    val graphics: String?,
    val storage: String?,
)
