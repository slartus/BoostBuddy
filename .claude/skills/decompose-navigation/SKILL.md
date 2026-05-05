# Skill: Добавление экрана в навигацию Decompose

## Когда использовать

- "добавь экран в навигацию"
- "настрой переход на экран X"
- "свяжи экран с навигацией"

## Шаги

### 1. Добавить Configuration

В `RootComponent.kt`:

```kotlin
sealed class Configuration {
    data object Blog : Configuration()
    data class NewScreen(val id: String? = null) : Configuration() // новый экран
    // ...
}
```

### 2. Добавить обработку в childStack

```kotlin
override val childStack = childStack(
    source = navigation,
    serializer = Configuration.serializer(),
    initialConfiguration = Configuration.Blog,
    handleBackButton = true,
) { configuration, componentContext ->
    when (configuration) {
        is Configuration.Blog -> {
            BlogComponent(
                componentContext = componentContext,
                onNavigateToNewScreen = { id ->
                    navigation.push(Configuration.NewScreen(id))
                },
            )
        }
        is Configuration.NewScreen -> {
            NewScreenComponent(
                componentContext = componentContext,
                id = configuration.id,
                onBack = { navigation.pop() },
            )
        }
    }
}
```

### 3. Добавить переход

В родительском компоненте:

```kotlin
private val onNavigateToNewScreen: (String?) -> Unit
    get() = { id ->
        navigation.push(Configuration.NewScreen(id))
    }
```

### 4. Обновить RootScreen

В `RootScreen.kt` добавить отображение нового экрана:

```kotlin
when (val child = state.activeChild.instance) {
    is Child.Blog -> BlogScreen(child.component)
    is Child.NewScreen -> NewScreen(component = child.component)
}
```

## Проверка

- Проверь, что back button работает
- Проверь передачу параметров (если есть)
- Сохрани состояние при навигации
