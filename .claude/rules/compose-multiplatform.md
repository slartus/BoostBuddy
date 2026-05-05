# Compose Multiplatform Rules

## Общие правила

- Используй `material3` вместо `material`
- Избегай платформенно-зависимого кода в `commonMain`
- Для ресурсов используй `Res.drawable.*` (compose-resources)

## Структура экрана

```kotlin
@Composable
fun ScreenName(
    component: ComponentName,
) {
    val state by component.viewStates.subscribeAsState()

    // Рендеринг состояния
    when (val s = state) {
        is State.Loading -> LoadingIndicator()
        is State.Content -> ContentView(s)
    }

    // Обработка действий
    SubscribeToActions(component.viewActions) { action ->
        when (action) {
            // ...
        }
    }
}
```

## Preview

```kotlin
@Preview
@Composable
fun ScreenNamePreview() {
    AppTheme {
        ScreenName(/* mock component */)
    }
}
```

## TestTags

Добавляй TestTags для ключевых элементов:

```kotlin
Modifier.testTag("screen_name_button_submit")
```

## Платформенные различия

- Android: используй `LocalContext.current`
- iOS: используй `LocalPlatformContext.current`
- Общий код: избегай `androidx.activity` или `UIKit`
