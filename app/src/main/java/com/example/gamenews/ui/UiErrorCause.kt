package com.example.gamenews.ui

import androidx.annotation.StringRes
import com.example.gamenews.R
import java.io.IOException

/** Why a load failed, reduced to the distinctions a screen actually makes. */
enum class UiErrorCause {
    NETWORK,
    UNKNOWN,
}

internal fun Throwable.toUiErrorCause(): UiErrorCause =
    if (this is IOException) UiErrorCause.NETWORK else UiErrorCause.UNKNOWN

@StringRes
internal fun UiErrorCause.messageRes(): Int = when (this) {
    UiErrorCause.NETWORK -> R.string.error_network
    UiErrorCause.UNKNOWN -> R.string.error_unknown
}
