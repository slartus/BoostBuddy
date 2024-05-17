package ru.slartus.boostbuddy.utils

import io.github.aakira.napier.Napier

suspend fun <T> fetchOrError(block: suspend () -> T): Result<T> {
    return runCatching {
        val data = block()

        Result.success(data)
    }.getOrElse {
        Napier.e("fetchOrError", it)
        Result.failure(it.toServerException())
    }
}