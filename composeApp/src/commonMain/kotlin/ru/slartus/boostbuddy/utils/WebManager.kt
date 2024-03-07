package ru.slartus.boostbuddy.utils

expect object WebManager {
    suspend fun clearWebViewCookies()
}