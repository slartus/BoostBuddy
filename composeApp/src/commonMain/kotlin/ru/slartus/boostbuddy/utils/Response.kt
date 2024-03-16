package ru.slartus.boostbuddy.utils

suspend fun <T> fetchOrError(block: suspend () -> T): Result<T> {
    return runCatching {
        val data = block()

        Result.success(data)
    }.getOrElse {
        Result.failure(it.toServerException())
    }
}