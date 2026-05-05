# Skill: Создание Compose экрана

## Когда использовать

- "создай экран X"
- "добавь новый экран"
- "реализуй фичу X"

## Шаги

### 1. Создать Component

Файл: `components/XComponent.kt`

```kotlin
package ru.slartus.boostbuddy.components.x

import com.arkivanov.decompose.ComponentContext
import ru.slartus.boostbuddy.components.BaseComponent

sealed class State {
    data object Loading : State()
    data class Content(val data: String) : State()
}

sealed class Event {
    data object BackClicked : Event()
}

sealed class Action {
    data class ShowError(val message: String) : Action()
}

class XComponent(
    componentContext: ComponentContext,
    private val onBack: () -> Unit,
) : BaseComponent(componentContext) {

    private val _viewStates = MutableValue(State.Loading)
    val viewStates: Value<State> = _viewStates

    private val _viewActions = actionBinder<Action>()
    val viewActions: ActionBinder<Action> = _viewActions

    init {
        loadData()
    }

    private fun loadData() {
        // TODO: load data
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.BackClicked -> onBack()
        }
    }
}
```

### 2. Создать Screen

Файл: `ui/screens/XScreen.kt`

```kotlin
package ru.slartus.boostbuddy.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.slartus.boostbuddy.components.x.XComponent

@Composable
fun XScreen(component: XComponent) {
    val state by component.viewStates.subscribeAsState()

    when (val s = state) {
        is State.Loading -> LoadingView()
        is State.Content -> ContentView(s)
    }

    SubscribeToActions(component.viewActions) { action ->
        when (action) {
            is Action.ShowError -> { /* показ ошибки */ }
        }
    }
}

@Composable
private fun ContentView(state: State.Content) {
    // UI implementation
}

@Preview
@Composable
fun XScreenPreview() {
    AppTheme {
        // Preview
    }
}
```

### 3. Добавить в навигацию

В `RootComponent.kt`:

- Добавить конфиг в `Configuration`
- Добавить обработку в `childStack`

### 4. Self-review

Запустить `code-reviewer` через Task tool.
