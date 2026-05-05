# Skill: Вызов API через Ktor

## Когда использовать

- "добавь вызов API"
- "получи данные с сервера"
- "сделай запрос к X"

## Паттерн

### 1. Создать модели (DTO)

```kotlin
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse(
    val data: List<ItemDto>,
)

@Serializable
data class ItemDto(
    val id: String,
    val title: String,
)
```

### 2. Маппер DTO -> Domain

```kotlin
fun ItemDto.toDomain(): Item = Item(
    id = id,
    title = title,
)
```

### 3. Repository с Ktor

```kotlin
class FeatureRepositoryImpl(
    private val httpClient: HttpClient,
) : FeatureRepository {

    override suspend fun getData(): List<Item> {
        return try {
            val response: ApiResponse = httpClient
                .get("api/v1/endpoint") {
                    parameter("key", "value")
                }
                .body()

            response.data.map { it.toDomain() }
        } catch (e: Exception) {
            throw NetworkException("Failed to fetch data", e)
        }
    }
}
```

### 4. Вызов в Component

```kotlin
class FeatureComponent(
    componentContext: ComponentContext,
) : BaseComponent(componentContext) {

    private val repository: FeatureRepository by inject()

    private fun loadData() {
        scope.launch {
            _viewStates.value = State.Loading
            try {
                val data = repository.getData()
                _viewStates.value = State.Content(data)
            } catch (e: Exception) {
                _viewActions.send(Action.ShowError(e.message ?: "Unknown error"))
            }
        }
    }
}
```

## Правила

- Используй `try-catch` для обработки ошибок сети
- Маппинг DTO -> Domain в отдельном методе
- Используй `scope.launch` для корутин в Component
- Парсинг JSON через Kotlinx Serialization (не вручную)
