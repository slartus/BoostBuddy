# Post-changes hook

## Когда запускается

После каждого блока изменений в коде (после Edit, Write, Bash операций).

## Что делает

1. Запускает `code-reviewer` субагента для ревью изменений
2. Проверяет соответствие проектным правилам:
    - Decompose Component паттерн (State, Event, Action)
    - Compose best practices
    - Ktor паттерны
    - Навигация через Decompose

## Как использовать

Автоматически вызывается через Task tool после завершения изменений.

## Пример вызова

```
Task(
  description="Self-review changes",
  prompt="Проведи ревью последних изменений. Проверь: Decompose паттерн, Compose, Ktor, навигацию. Используй правила из .claude/rules/",
  subagent_type="code-reviewer"
)
```
