package com.example.gamenews.domain.model

/** Every field is optional: the API omits them for non-PC titles. */
data class SystemRequirements(
    val os: String?,
    val processor: String?,
    val memory: String?,
    val graphics: String?,
    val storage: String?,
)
