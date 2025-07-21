package ru.slartus.boostbuddy.data.log

class CompositeLogger(private val loggers: List<Logger>) : Logger {
    override fun d(message: String) = loggers.forEach { it.d(message) }

    override fun d(message: () -> String) = loggers.forEach { it.d(message) }

    override fun i(message: String) = loggers.forEach { it.i(message) }

    override fun i(message: () -> String) = loggers.forEach { it.i(message) }

    override fun w(message: String) = loggers.forEach { it.w(message) }

    override fun w(message: () -> String) = loggers.forEach { it.w(message) }

    override fun w(throwable: Throwable, message: String) =
        loggers.forEach { it.w(throwable, message) }

    override fun w(throwable: Throwable, message: () -> String) =
        loggers.forEach { it.w(throwable, message) }

    override fun e(message: String) = loggers.forEach { it.e(message) }

    override fun e(message: () -> String) = loggers.forEach { it.e(message) }

    override fun e(throwable: Throwable, message: String) =
        loggers.forEach { it.e(throwable, message) }

    override fun e(throwable: Throwable, message: () -> String) =
        loggers.forEach { it.e(throwable, message) }
}