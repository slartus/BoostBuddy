package ru.slartus.boostbuddy.navigation

internal fun <C> persistableStack(stack: List<C>, isTransient: (C) -> Boolean): List<C>? =
    stack.takeWhile { !isTransient(it) }.takeIf { it.isNotEmpty() }
