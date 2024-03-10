package ru.slartus.boostbuddy.utils

import ru.slartus.boostbuddy.data.Inject


object IosDirectDependencies {
    val exceptionsHandler: GlobalExceptionHandlersChain get() = Inject.instance()
}
