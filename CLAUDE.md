# Claude Code для BoostBuddy

## Что это за проект

- **BoostBuddy** — Kotlin Multiplatform приложение (Android + iOS)
- Архитектура: **Decompose** (Component вместо ViewModel)
- UI: **Compose Multiplatform**
- Навигация: Decompose `RootComponent` + `childStack`
- Сети: **Ktor** с Kotlinx Serialization
- DI: **Kodein**
- Пакет: `ru.slartus.boostbuddy`

## Структура проекта

```
composeApp/src/commonMain/kotlin/ru/slartus/boostbuddy/
├── components/          # Decompose Components (логика экранов)
│   ├── RootComponent.kt
│   ├── settings/SettingsComponent.kt
│   ├── blog/BlogComponent.kt
│   └── ...
├── ui/screens/          # Compose UI экраны
│   ├── SettingsScreen.kt
│   ├── blog/BlogScreen.kt
│   └── ...
├── navigation/          # Навигационные конфигы
├── ui/theme/           # Тема приложения
└── utils/              # Утилиты
```

## Приоритет правил

1. Прямой запрос пользователя
2. `.claude/rules/` — проектные правила
3. `~/.claude/agents/` — глобальные агенты (KMP, Decompose)
4. Этот файл
5. Глобальный `~/.claude/CLAUDE.md`

## Как работать в этом проекте

- **Компоненты**: используй `kmp-presentation-agent` (Decompose Component паттерн)
- **UI**: используй `kmp-ui-agent` (Compose Multiplatform)
- **Навигация**: используй `kmp-navigation-agent` (Decompose)
- **Сеть**: Ktor клиент, смотри существующие `components/` для паттернов
- **DI**: Kodein, смотри `BaseComponent.kt` для примера

## Agents

### Consilium

| Role        | Agent             |
|-------------|-------------------|
| architect   | kmp-architect     |
| frontend    | kmp-ui-agent      |
| ui          | kmp-ui-agent      |
| test        | kmp-test-agent    |
| diagnostics | kotlin-specialist |

### Executing

| Agent                  | Scope                                      |
|------------------------|--------------------------------------------|
| kmp-architect          | **/*.kt (архитектура)                      |
| kmp-presentation-agent | **/components/**/*.kt                      |
| kmp-ui-agent           | **/ui/**/*.kt, **/screens/**/*.kt          |
| kmp-navigation-agent   | **/navigation/**/*.kt, **/RootComponent.kt |
| kmp-test-agent         | **/*Test.kt                                |
| kotlin-specialist      | **/*.kt                                    |
| code-reviewer          | **/*.kt                                    |

### Models

| Role      | Model  |
|-----------|--------|
| architect | opus   |
| *         | sonnet |

## Обязательный self-review

После изменений запускай `code-reviewer` через Task tool.
Проверяй:

- Decompose Component паттерн (State, Events, Actions)
- Compose best practices
- Ktor паттерны (общение с API)
- Навигация через Decompose

## Что не трогать

- `build/` директории
- Сгенерированные файлы
- `keystore/` (секреты)
- `gradle/` (инфраструктура)

## Полезные команды

- Сборка Android: `./gradlew assembleDebug`
- Запуск тестов: `./gradlew allTests`
- Сборка iOS framework: `./gradlew assembleSharedReleaseXCFramework`
