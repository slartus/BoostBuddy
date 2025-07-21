package ru.slartus.boostbuddy.utils

import ru.slartus.boostbuddy.data.log.logger

suspend fun <T> fetchOrError(block: suspend () -> T): Result<T> {
    return runCatching {
        val data = block()

        Result.success(data)
    }.getOrElse {
        logger.e("fetchOrError", it)
        Result.failure(it.toServerException())
    }
}