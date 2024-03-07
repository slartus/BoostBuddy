package ru.slartus.boostbuddy.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value

abstract class BaseComponent<State : Any>(
    componentContext: ComponentContext,
    initState: State,
) : ComponentContext by componentContext {
    protected val scope = coroutineScope()
    private val _state: MutableValue<State> = MutableValue(initState)
    val viewStates: Value<State> = _state

    protected var viewState: State
        get() = _state.value
        set(value) {
            _state.value = value
        }
}