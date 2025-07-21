package ru.slartus.boostbuddy.data.log

import ru.slartus.boostbuddy.data.Inject

val logger: Logger
    get() = Inject.instance()

interface Logger {
    fun d(message: String)
    fun d(message: () -> String)
    fun i(message: String)
    fun i(message: () -> String)
    fun w(message: String)
    fun w(message: () -> String)
    fun w(throwable: Throwable, message: String)
    fun w(throwable: Throwable, message: () -> String)
    fun e(message: String)
    fun e(message: () -> String)
    fun e(throwable: Throwable, message: String)
    fun e(throwable: Throwable, message: () -> String)
    fun e(message: String, throwable: Throwable) = e(throwable, message)
}
