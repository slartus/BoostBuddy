package ru.slartus.boostbuddy.data.log

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

class NapierProxy : Logger {
    override fun d(message: String) = Napier.d(message)

    override fun d(message: () -> String) = Napier.d(message = message)

    override fun i(message: String) = Napier.i(message)

    override fun i(message: () -> String) = Napier.i(message = message)

    override fun w(message: String) = Napier.w(message = message)

    override fun w(message: () -> String) = Napier.w(message = message)

    override fun w(throwable: Throwable, message: String) =
        Napier.i(message = message, throwable = throwable)

    override fun w(throwable: Throwable, message: () -> String) =
        Napier.i(message = message, throwable = throwable)

    override fun e(message: String) = Napier.e(message = message)

    override fun e(message: () -> String) = Napier.e(message = message)

    override fun e(throwable: Throwable, message: String) =
        Napier.e(message = message, throwable = throwable)

    override fun e(throwable: Throwable, message: () -> String) =
        Napier.e(message = message, throwable = throwable)

}

fun debugLogBuild() {
    Napier.base(DebugAntilog())
}