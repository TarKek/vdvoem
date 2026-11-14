# AGENTS.md — правила работы над «Вдвоём»

> Читается автоматически в каждой сессии. Держать коротко и по делу.
> Продуктовые детали — в [ROADMAP.md](ROADMAP.md), история и окружение — в [NOTES.md](NOTES.md).

## Проект

Android-приложение для пары: календарь, желания, подарки + магазин подключаемых модулей.
Название: **Вдвоём**, кодовое — **Tandem**, пакет `app.vdvoem`.
Стиль: **чистый Material 3 (1.3.x). Expressive не используем.**
Язык интерфейса и документации: русский. Строки UI — в `strings.xml`, не хардкодить в Compose.
MVP: календарь, желания, подарки, магазин модулей (ROADMAP §3).

## Архитектура — обязательные правила

- Single-activity Compose, Navigation Compose, тема и токены в `:core:ui`.
- Модули Gradle: `:core:ui`, `:core:domain`, `:core:data`, `:core:sync`, `:feature:*`, `:module:*`.
- Контракт `AppModule` (ROADMAP §2): ядро **не импортирует** фичи, фичи **не знают** друг о друге.
- Нижние вкладки и навигация собираются из `ModuleRegistry` по включённым модулям.
- Состояние модулей — DataStore: `enabled`, `order`, `settings` (JSON).
- Данные — Room + репозитории в `:core:data`; домен без Android-зависимостей.
- **Local-first, sync-ready:** каждая фича работает офлайн, но обмен данными между партнёрами — ключевая
  ценность. Все сущности: `UUID`, `pairId`, `updatedAt`, `deletedAt` (tombstone), версия; запись только
  через репозитории, чтобы позже включить офлайн-очередь и синк без переделки модели.
- DI — Koin. Разрешения — из `AndroidManifest.xml` по мере надобности, не заранее.
- Никаких «серых» API в ядре: Steam — изолированный модуль, выключен по умолчанию.

## Дизайн — обязательные правила

- Никаких хардкод-цветов, размеров и шрифтов в экранах: только `MaterialTheme` и токены `VdvoemTheme`
  (spacing, radius, elevation, motion).
- Свои обёртки в `:core:ui/components` (AppCard, AppButton, SectionHeader, EmptyState, …):
  смена дизайна должна быть правкой обёрток и токенов, а не 40 экранов.
- Светлая и тёмная тема с первого экрана. Dynamic color на Android 12+, иначе брендовая палитра.
- Каждый компонент — с `@Preview`; итоговый экран проверять на устройстве скриншотом.

## Окружение (критично)

- Windows + PowerShell. Scratch: `C:\Users\Administrator\AppData\Local\Temp\opencode`.
- **`JAVA_HOME` в системе битый** (`Zulu\zulu-11-jre`, падает с `lib/ext`). В каждой сессии:
  `$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"`.
  В `gradle.properties` также прописать `org.gradle.java.home=...` (с прямыми слэшами).
- Gradle 8.9 (wrapper нет): `C:\Users\Administrator\AppData\Local\Temp\opencode\gradle-8.9\bin\gradle.bat`.
  Если папки нет — скачать `https://services.gradle.org/distributions/gradle-8.9-bin.zip` (136 МБ).
- Android SDK: `C:\Users\Administrator\AppData\Local\Android\Sdk`. **Не переносить, не чистить.**
  Есть: `platforms/android-35`, `build-tools/36.0.0` и `34.0.0`, `cmdline-tools/latest`, `platform-tools`, NDK, CMake.
- `local.properties`: `sdk.dir=C\:\\Users\\Administrator\\AppData\\Local\\Android\\Sdk`.
- adb: `C:\adb\adb.exe`. Устройство: `3089b0c3` — Xiaomi 2109119DG, Android 17 (API 37), 1080×2400, arm64-v8a.
- Проверенные версии (не менять без причины): AGP 8.7.3, Kotlin 2.0.21 + compose-плагин 2.0.21,
  Compose BOM 2024.10.01 (material3 1.3.1), compileSdk 35, minSdk 26, JDK 21, Gradle 8.9.
- На C: свободно ~9 ГБ: большие SDK-компоненты/эмуляторы не качать без спроса; предупреждать о загрузках >50 МБ.

## Типовые команды

```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
& "C:\Users\Administrator\AppData\Local\Temp\opencode\gradle-8.9\bin\gradle.bat" :app:assembleDebug --console=plain
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -W -n app.vdvoem/.MainActivity
adb logcat -d --pid=$(adb shell pidof app.vdvoem) -t 500 | Select-String "FATAL|AndroidRuntime|ANR"
adb shell screencap -p /sdcard/shot.png; adb pull /sdcard/shot.png .
```

## Процесс и качество

- Перед крупной работой сверяться с ROADMAP; по завершении отмечать пункты и обновлять NOTES.
- Definition of Done для фичи: собирается, запускается, нет `FATAL`/`ANR` в logcat, работают тёмная/светлая
  тема, поворот и перезапуск процесса (state сохранён), есть скриншот с устройства.
- Не трогать файлы вне рабочей папки. Ничего не перемещать на другие диски.
- Ответы пользователю: по-русски, коротко, без воды; о рисках и скачиваниях предупреждать заранее.
