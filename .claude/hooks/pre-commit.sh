#!/bin/bash
# Pre-commit hook для BoostBuddy
# Запускается перед каждым commit

set -e

echo "🔍 Running pre-commit checks..."

# 1. Проверка, что проект собирается
echo "📦 Building project..."
./gradlew assembleDebug --no-daemon --quiet

# 2. Запуск тестов (опционально, можно закомментировать для ускорения)
# echo "🧪 Running tests..."
# ./gradlew allTests --no-daemon --quiet

# 3. Проверка на секреты в коде
echo "🔒 Checking for secrets..."
if git diff --cached --name-only | xargs grep -l -E "(password|secret|api_key|token)" 2>/dev/null; then
    echo "⚠️ Warning: Possible secrets found in staged files"
    echo "   Review changes before committing"
fi

# 4. Проверка запрещённых файлов
echo "🚫 Checking forbidden files..."
FORBIDDEN_FILES=$(git diff --cached --name-only | grep -E "(keystore/|.gradle/|build/)" || true)
if [ -n "$FORBIDDEN_FILES" ]; then
    echo "❌ Error: Attempt to commit forbidden files:"
    echo "$FORBIDDEN_FILES"
    exit 1
fi

echo "✅ Pre-commit checks passed!"
