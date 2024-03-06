package ru.slartus.boostbuddy.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.SubscribeItem
import ru.slartus.boostbuddy.data.repositories.SubscribesRepository
import ru.slartus.boostbuddy.data.repositories.getAccessToken
import ru.slartus.boostbuddy.utils.unauthorizedError


interface SubscribesComponent {
    val state: Value<SubscribesViewState>
    fun onItemClicked(item: SubscribeItem)
}

data class SubscribesViewState(
    val items: List<SubscribeItem>,
)

class SubscribesComponentImpl(
    componentContext: ComponentContext,
    private val onItemSelected: (item: SubscribeItem) -> Unit,
) : SubscribesComponent, ComponentContext by componentContext {
    private val scope = coroutineScope()
    private val settingsRepository by Inject.lazy<SettingsRepository>()
    private val subscribesRepository by Inject.lazy<SubscribesRepository>()

    private val _state: MutableStateFlow<SubscribesViewState> = MutableStateFlow(
        SubscribesViewState(
            emptyList()
        )
    )

    override val state: Value<SubscribesViewState> =
        _state.asValue(initialValue = _state.value, lifecycle = lifecycle)

    init {
        fetchSubscribes()
    }

    private fun fetchSubscribes() {
        scope.launch {
            val token = settingsRepository.getAccessToken() ?: unauthorizedError()
            runCatching {
                val items = subscribesRepository.getSubscribes(token)
                _state.value = state.value.copy(items = items)
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    override fun onItemClicked(item: SubscribeItem) {
        onItemSelected(item)
    }
}