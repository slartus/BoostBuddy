package ru.slartus.boostbuddy.data.repositories

import io.ktor.client.HttpClient

internal class GithubRepository(
    private val httpClient: HttpClient
) {
}