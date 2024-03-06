package ru.slartus.boostbuddy.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.launch
import ru.slartus.boostbuddy.data.Inject
import ru.slartus.boostbuddy.data.repositories.SettingsRepository
import ru.slartus.boostbuddy.data.repositories.SubscribeItem
import ru.slartus.boostbuddy.data.repositories.SubscribesRepository
import ru.slartus.boostbuddy.utils.Response
import ru.slartus.boostbuddy.utils.messageOrThrow
import ru.slartus.boostbuddy.utils.unauthorizedError


interface SubscribesComponent {
    val state: Value<SubscribesViewState>
    fun onItemClicked(item: SubscribeItem)
}

data class SubscribesViewState(
    val progressProgressState: ProgressState,
) {
    companion object {
        internal fun init(): SubscribesViewState = SubscribesViewState(ProgressState.Init)
        internal fun loading(): SubscribesViewState = SubscribesViewState(ProgressState.Loading)
        internal fun loaded(items: List<SubscribeItem>): SubscribesViewState =
            SubscribesViewState(ProgressState.Loaded(items))

        internal fun error(description: String): SubscribesViewState =
            SubscribesViewState(ProgressState.Error(description))
    }

    sealed class ProgressState {
        data object Init : ProgressState()
        data object Loading : ProgressState()
        data class Loaded(val items: List<SubscribeItem>) : ProgressState()
        data class Error(val description: String) : ProgressState()
    }
}

class SubscribesComponentImpl(
    componentContext: ComponentContext,
    private val onItemSelected: (item: SubscribeItem) -> Unit,
) : SubscribesComponent, ComponentContext by componentContext {
    private val scope = coroutineScope()
    private val settingsRepository by Inject.lazy<SettingsRepository>()
    private val subscribesRepository by Inject.lazy<SubscribesRepository>()

    private val _state = MutableValue(SubscribesViewState.init())
    override var state: Value<SubscribesViewState> = _state

    init {
        checkToken()
        subscribeToken()
    }

    private fun checkToken() {
        scope.launch {
            if (settingsRepository.getAccessToken() == null)
                unauthorizedError()
        }
    }

    private fun subscribeToken() {
        scope.launch {
            settingsRepository.tokenFlow.collect { token ->
                if (token != null)
                    fetchSubscribes(token)
            }
        }
    }

    private fun fetchSubscribes(token: String) {
        _state.value = SubscribesViewState.loading()
        scope.launch {
            when (val response = subscribesRepository.getSubscribes(token)) {
                is Response.Error -> _state.value =
                    SubscribesViewState.error(response.exception.messageOrThrow())

                is Response.Success -> _state.value = SubscribesViewState.loaded(response.data)
            }
        }
    }

    override fun onItemClicked(item: SubscribeItem) {
        onItemSelected(item)
    }
}