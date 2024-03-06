package ru.slartus.boostbuddy.utils

sealed class Response<T> {
    data class Success<T>(val data: T) : Response<T>()
    data class Error<T>(val exception: ServerException) : Response<T>()
}

suspend fun <T> fetchOrError(block: suspend () -> T): Response<T> {
    return runCatching {
        val data = block()

        Response.Success(data)
    }.getOrElse {
        Response.Error(it.toServerException())
    }
}