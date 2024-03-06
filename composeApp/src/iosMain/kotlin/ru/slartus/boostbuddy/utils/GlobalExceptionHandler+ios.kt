package ru.slartus.boostbuddy.utils

import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
actual fun registerGlobalExceptionHandler(handler: GlobalExceptionHandlersChain) {

    val currentHook = getUnhandledExceptionHook()

    setUnhandledExceptionHook { error ->
        try {
            if (!handler(error))
                throw error
        } catch (e: Throwable) {
            currentHook?.invoke(e)
        }
    }
}