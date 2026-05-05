# Ktor + Kodein Pattern

## Ktor клиент

Используй уже настроенный клиент из `BaseComponent` или DI:

```kotlin
class FeatureComponent(
    componentContext: ComponentContext,
) : BaseComponent(componentContext) {

    private val httpClient: HttpClient by inject()

    suspend fun fetchData() {
        try {
            val response: ApiResponse = httpClient.get("endpoint") {
                parameter("key", "value")
            }.body()
        } catch (e: Exception) {
            // обработка ошибок
        }
    }
}
```

## Kodein DI

Инъекция зависимостей через `inject()`:

```kotlin
class FeatureComponent(
    componentContext: ComponentContext,
) : BaseComponent(componentContext) {

    private val repository: FeatureRepository by inject()
    private val settings: AppSettings by inject()
}
```

## Определение зависимостей

В `di/` модуле:

```kotlin
bind<FeatureRepository>() with singleton {
    FeatureRepositoryImpl(instance())
}
```

## Правила

- Не создавай `HttpClient` напрямую в Component — используй DI
- Используй Kotlinx Serialization для JSON
- Обрабатывай ошибки сети через try-catch
- Для авторизации используй `Auth` плагин Ktor
