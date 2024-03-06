package ru.slartus.boostbuddy.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value


interface SubscribesComponent {
    val model: Value<Model>

    fun onItemClicked(item: String)

    data class Model(
        val items: List<String>,
    )
}

class SubscribesComponentImpl(
    componentContext: ComponentContext,
    private val onItemSelected: (item: String) -> Unit,
) : SubscribesComponent, ComponentContext by componentContext {
    private val scope = coroutineScope()
    override val model: Value<SubscribesComponent.Model> =
        MutableValue(SubscribesComponent.Model(items = List(100) { "Item $it" }))

    override fun onItemClicked(item: String) {
        onItemSelected(item)
    }
}