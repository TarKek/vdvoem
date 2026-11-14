# NOTES — рабочие заметки

Живой файл: сюда пишем решения, найденные подводные камни и следующие шаги.
Продуктовые требования — в [ROADMAP.md](ROADMAP.md).

## Состояние на 2026-11-14

- Есть только документация: `README.md`, `ROADMAP.md`, `AGENTS.md`, `NOTES.md`. Кода приложения ещё нет.
- Следующий шаг — Фаза 0: каркас Gradle-проекта, тема/токены, навигация из реестра модулей,
  экран «Магазин дополнений» с заглушками, Room-схема MVP.

## Решения

| Дата | Решение | Почему |
|---|---|---|
| 2026-11-14 | Название «Вдвоём», кодовое Tandem | нравятся оба, тёплое и международное |
| 2026-11-14 | Material 3 без Expressive | стабильные компоненты, меньше риска при обновлениях |
| 2026-11-14 | Синк пары — критичная функция, не «потом»; сейчас нет хостинга → sync-ready модель + ручной обмен | ключевая ценность приложения для пары |
| 2026-11-14 | Local-first | быстрый MVP без серверной инфраструктуры |
| 2026-11-14 | Модули внутри одного APK | проще, чем dynamic feature/внешние APK |
| 2026-11-14 | Музыка — свой сервер (стрим/скачивание) | у Spotify/Яндекс.Музыки нет открытых API |
| 2026-11-14 | Steam — полный клиент, но отдельным модулем позже | неофициальные API, риск для аккаунта |
| 2026-11-14 | Токены + обёртки компонентов | смена дизайна по референсам должна быть дешёвой |

## Окружение (проверено на практике)

- **Java:** рабочий JDK 21 — `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`.
  Системный `JAVA_HOME` указывает на `C:\Program Files\Zulu\zulu-11-jre\` и **ломает** gradle
  (`<JAVA_HOME>/lib/ext exists ... Could not create the Java Virtual Machine`).
  Решение: выставлять `JAVA_HOME` в каждой сессии и/или `org.gradle.java.home` в `gradle.properties`.
- **Gradle:** глобально не установлен. Скачан `gradle-8.9-bin.zip` (136 МБ) в
  `C:\Users\Administrator\AppData\Local\Temp\opencode\gradle-8.9`. Wrapper в проектах пока не создавали.
- **Android SDK:** `C:\Users\Administrator\AppData\Local\Android\Sdk` (2,7 ГБ).
  `platforms/android-35`, `build-tools/36.0.0` + `34.0.0` (34 доустановил AGP автоматически),
  `cmdline-tools/latest`, `platform-tools`, NDK, CMake, licenses приняты.
  ⚠️ SDK один раз переносили на `E:\Android\Sdk` и вернули обратно по просьбе пользователя — **больше не переносить**.
- **Свободное место:** C: ≈ 8,9 ГБ, E: ≈ 254 ГБ, F: ≈ 285 ГБ. На C: тесно → большие загрузки только с согласия.
- **adb:** `C:\adb\adb.exe`. Устройство `3089b0c3` — Xiaomi 2109119DG, Android 17 (API 37), 1080×2400, 440 dpi, arm64-v8a.
- **Проверенные версии сборки:** AGP 8.7.3, Gradle 8.9, Kotlin 2.0.21 (+ `org.jetbrains.kotlin.plugin.compose`),
  Compose BOM 2024.10.01 (material3 1.3.1), activity-compose 1.9.3, lifecycle 2.8.7, core-ktx 1.13.1,
  material 1.12.0 (для XML-темы), compileSdk 35, minSdk 26, JDK 21.
- **Грабли:** `org.gradle.java.home` нельзя задавать с обратными слэшами; `local.properties` — экранировать
  двоеточие (`C\:\\...`). `adb exec-out screencap -p > file.png` в PowerShell портит бинарник —
  надёжнее `adb shell screencap -p /sdcard/x.png` + `adb pull`.

## Полезный референс

- `E:\ai\workspace\testmaterial3build` — готовое тестовое приложение на Compose + Material 3
  (3 экрана: дашборд, компоненты, настройки; dynamic color, тёмная тема, snackbar, диалоги).
  Собрано и проверено на устройстве 3089b0c3. Можно переиспользовать паттерны: тема, Scaffold,
  NavigationBar, карточки, чипы, `TopAppBar` + `nestedScroll`.
- Уроки оттуда: Material 3 1.3.1 требует `@OptIn(ExperimentalMaterial3Api::class)` для `TopAppBar` и
  `SegmentedButton`; `LinearProgressIndicator(progress = { … })` — новый API; иконки только из
  `material-icons-core` (LightMode/DarkMode там нет); FAB перекрывает список — нужен нижний отступ ~96 dp.
- Холодный старт тестового приложения: ~1,07 с, 51 пропущенный кадр на первом кадре (debug), PSS 156 МБ,
  падений и ANR не было. Ориентир для «Вдвоём».

## Открытые вопросы (перенесено из ROADMAP §9)

- [ ] Монетизация: бесплатно / донаты / подписка на синк?
- [ ] Фото и аватары в MVP или позже?
- [ ] Языки: RU или сразу RU + EN?
- [ ] Хостинг бэкенда и медиасервера (VPS / домашний сервер / NAS)?
- [ ] minSdk 26 или 28?
- [ ] Дизайн-референсы: что нравится пользователю (скриншоты/ссылки)?
- [ ] Пакет: `app.vdvoem` или `app.tandem.vdvoem`?

## Журнал

- **2026-11-14** — собрали и отладили тестовое Material 3 приложение (`testmaterial3build`),
  развернули на телефон, проверили темы, диалоги, snackbar, dynamic color.
- **2026-11-14** — провели интервью по «Вдвоём», зафиксировали scope, создали `ROADMAP.md`
  и папку проекта `E:\ai\workspace\vdvoem`.
- **2026-11-14** — отказались от Expressive в пользу стабильного Material 3; добавлены `README.md`,
  `AGENTS.md`, `NOTES.md`. Кодовой базы пока нет.
- **2026-11-14** — создан репозиторий `https://github.com/TarKek/vdvoem` (private), запушен `main`.
- **2026-11-14** — Фаза 0: 8 Gradle-модулей (`:app`, `:core:ui/domain/data`, `:feature:calendar/wishes/gifts/store`),
  брендовая тема и токены, Koin, DataStore для состояния модулей, навигация из реестра, магазин дополнений
  с реальным вкл/выкл и разделом «скоро». `assembleDebug` и `installDebug` — успешно, cold start ~1,05 с.
  Room отложен в Фазу 1 (не тащим мёртвый код). Dynamic color — параметр темы, по умолчанию брендовая палитра.
- **2026-11-14** — Фаза 1 (данные + календарь): Room 2.6.1 через KSP, таблицы `pair` и `events`
  с `pairId/updatedAt/deletedAt`, репозитории в `:core:data`, `Dates` в домене (годовщины, UTC-полуночи
  DatePicker, русские даты). Настройка пары (имена + «вместе с») на главной, реальный календарь:
  добавление/удаление событий, ежегодные даты, отсчёт в hero-карточке. Сборка и запуск — успешно,
  cold start 0,89 с, падений нет. Версия 0.2.0.
