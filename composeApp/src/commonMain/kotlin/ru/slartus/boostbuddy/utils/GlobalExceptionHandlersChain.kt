package ru.slartus.boostbuddy.utils

import kotlinx.datetime.Clock


typealias GlobalExceptionHandler = (Throwable) -> Boolean

class GlobalExceptionHandlersChain {
    private val handlers: LinkedHashMap<Long, GlobalExceptionHandler> = LinkedHashMap()

    init {
        registerGlobalExceptionHandler(this)
    }

    fun registerHandler(handler: GlobalExceptionHandler): Long {
        val newHandlerId = Clock.System.now().epochSeconds
        handlers[newHandlerId] = handler
        return newHandlerId
    }

    fun unregisterHandler(handlerId: Long): Boolean {
        return handlers.remove(handlerId) != null
    }

    operator fun invoke(error: Throwable): Boolean {
        return handlers.entries.reversed().firstOrNull { it.value.invoke(error) } != null
    }
}