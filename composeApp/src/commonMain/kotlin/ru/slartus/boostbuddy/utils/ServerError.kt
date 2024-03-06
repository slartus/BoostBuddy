package ru.slartus.boostbuddy.utils

import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode

sealed class ServerException : Exception() {
    data object Unauthorized : ServerException()
    data class Request(val code: HttpStatusCode, override val message: String?) : ServerException()
    data class Other(override val message: String?) : ServerException()
}

fun Throwable.toServerException(): ServerException =
    when (this) {
        is ResponseException -> when (response.status) {
            HttpStatusCode.Unauthorized -> ServerException.Unauthorized

            else -> ServerException.Request(
                code = response.status,
                message = message ?: response.status.description
            )
        }

        else -> ServerException.Other(message)

    }

fun ServerException.messageOrThrow(): String {
    val message = when (this) {
        is ServerException.Unauthorized -> {
            unauthorizedError()
        }

        is ServerException.Request -> {
            message ?: code.toString()
        }

        is ServerException.Other -> {
            toString()
        }
    }
    return message
}