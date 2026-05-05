# Decompose Component Pattern

## Обязательная структура Component

Каждый экран/фича должна иметь Component с тремя сущностями:

```kotlin
// 1. State — неизменяемое состояние
sealed class State {
    data object Loading : State()
    data class Content(
        val title: String,
        val items: List<Item>,
        // ...
    ) : State()
}

// 2. Events — события от пользователя
sealed class Event {
    data object BackClicked : Event()
    data class ItemClicked(val id: String) : Event()
}

// 3. Actions — одноразовые эффекты (navigation, toasts, etc.)
sealed class Action {
    data class NavigateToDetails(val id: String) : Action()
    data class ShowError(val message: String) : Action()
}
```

## Структура файла Component

```kotlin
class FeatureComponent(
    componentContext: ComponentContext,
    private val onBack: () -> Unit,
) : BaseComponent(componentContext) {

    private val _viewStates = MutableValue(State())
    val viewStates: Value<State> = _viewStates

    private val _viewActions = actionBinder<Action>()
    val viewActions: ActionBinder<Action> = _viewActions

    init {
        loadData()
    }

    private fun loadData() {
        // ...
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.BackClicked -> onBack()
            is Event.ItemClicked -> _viewActions.send(Action.NavigateToDetails(event.id))
        }
    }
}
```

## В UI слое

```kotlin
@Composable
fun FeatureScreen(component: FeatureComponent) {
    val state by component.viewStates.subscribeAsState()

    when (val currentState = state) {
        is State.Loading -> LoadingView()
        is State.Content -> ContentView(currentState)
    }

    SubscribeToActions(component.viewActions) { action ->
        when (action) {
            is Action.NavigateToDetails -> { /* навигация */ }
            is Action.ShowError -> { /* показ ошибки */ }
        }
    }
}
```

## Правила

- State, Event, Action — sealed class (не data class по умолчанию)
- Component наследуется от `BaseComponent`
- Используй `actionBinder` для одноразовых эффектов
- Не храни одноразовые эффекты в State
- Подписывайся на state через `subscribeAsState()` в Compose
