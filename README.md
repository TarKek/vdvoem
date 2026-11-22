# Вдвоём · Tandem

Приватное Android-приложение для пары: общий календарь, желания, подарки и «магазин дополнений»,
где каждая пара включает только нужные модули. Плюс личные модули — музыка и Steam.

**Статус:** рабочая сборка 0.16.0 (Android), свой сервер синхронизации на VPS.

## Документация

| Файл | Что внутри |
|---|---|
| [ROADMAP.md](ROADMAP.md) | Продукт, MVP, архитектура, каталог модулей, фазы 0–6, бэклог идей |
| [NOTES.md](NOTES.md) | Рабочие заметки: окружение, решения, история, следующие шаги |
| [docs/PAIRING.md](docs/PAIRING.md) | Гайд тестеру: установка, создание пары, проверка синхронизации, сброс |
| [docs/PLUGIN_SDK.md](docs/PLUGIN_SDK.md) | Контракт плагинов, общие и личные модули, как предложить свой через GitHub |
| [AGENTS.md](AGENTS.md) | Правила для AI-агентов и конвенции разработки |

## Что делаем первым (MVP v0.1)

- Календарь пары: события, повторения, напоминания
- Желания: приоритет, ссылка, режим «сюрприз»
- Подарки: идея → куплено → подарено, бюджет
- Магазин дополнений: включение/выключение модулей и их настройки
- Material 3, светлая/тёмная тема, динамические цвета
- Local-first: всё работает без сети
- Синхронизация пары — ключевая функция: модель данных готова к синку с первого дня, сервер — как появится хостинг

## Стек (зафиксирован)

- Kotlin 2.0.21, AGP 8.7.3, Gradle 8.9, JDK 21
- Jetpack Compose (BOM 2024.10.01), Material 3 1.3.1 — без Expressive
- compileSdk 35, minSdk 26, targetSdk 35
- Room + KSP, DataStore, Navigation Compose, WorkManager, Koin
- Пакет: `app.vdvoem`

## Структура (план)

```
:app
:core:ui        тема, токены, обёртки компонентов
:core:domain    модели и use cases, контракт модулей
:core:data      Room, DataStore, репозитории
:core:sync      контракт синка и офлайн-очередь (сервер позже, модель готова сразу)
:feature:pair
:feature:calendar
:feature:wishes
:feature:gifts
:feature:store
:module:music   (позже)
:module:steam   (позже)
```

## Сборка (после создания каркаса)

```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
& "C:\Users\Administrator\AppData\Local\Temp\opencode\gradle-8.9\bin\gradle.bat" :app:assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -W -n app.vdvoem/.MainActivity
```

Подробности про окружение и подводные камни — в [NOTES.md](NOTES.md).
