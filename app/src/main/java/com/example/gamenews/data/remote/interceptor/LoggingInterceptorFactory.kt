package com.example.gamenews.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Body-level HTTP logging is only ever enabled in debug builds; release builds get a
 * NONE-level interceptor so no request or response content reaches logcat.
 */
object LoggingInterceptorFactory {

    fun create(isDebugBuild: Boolean): Interceptor = HttpLoggingInterceptor().apply {
        level = if (isDebugBuild) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }
}
