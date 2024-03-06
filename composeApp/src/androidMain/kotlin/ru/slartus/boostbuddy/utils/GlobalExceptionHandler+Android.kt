package ru.slartus.boostbuddy.utils

actual fun registerGlobalExceptionHandler(handler: GlobalExceptionHandlersChain){
    val defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, error ->
        try {
            if (!handler(error))
                throw error
        } catch (e: Throwable) {
            defaultUncaughtExceptionHandler?.uncaughtException(thread, e)
        }
    }
}