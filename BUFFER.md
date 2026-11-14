# BUFFER — общий буфер правок

Короткий журнал для параллельной работы. **Одна строка на правку**, без описаний.

Формат: `[кто] область — что сделал; что нужно от других`

Правила:
- Только свои файлы. Общие файлы (`app/`, `core/`, `BUFFER.md` кроме своей строки) не трогать.
- Нужна регистрация в DI / общий контракт — пиши строку `NEED:` и продолжай работу.
- Максимум 3 строки на агента. Старое не удалять, новое добавлять сверху в свой блок.

## Журнал

[settings] feature/settings — готово: SettingsModule (`core.settings`, route `settings`, CORE, order 95), SettingsViewModel, экран (тема, Material You, данные пары, о приложении, @Preview); NEED: app/build.gradle.kts dependency + регистрация SettingsViewModel + применение темы в MainActivity + ModuleRegistry.

[tests] core/domain+app — JVM-тесты DatesTest (15) и PairBackupTest (7) в src/test, только testImplementation (junit 4.13.2, kotlin-test 2.0.21, org.json 20240303 в app); :core:domain:testDebugUnitTest :app:testDebugUnitTest — зелёные; NEED: —
[tests] BUFFER — багов в Dates/PairBackup не найдено (leap day 29.02 в невисокосный год становится 28.02 — поведение JDK, не баг); NEED: —

[calendar-v2] feature/calendar — готово: правка события по тапу, удаление с подтверждением, фильтры Все/Сегодня/7 дней/Месяц, сортировка по ближайшей дате, пустые состояния; NEED: —
[store] feature/store — готово: поиск, фильтр категорий, включить/выключить все с подтверждением, EmptyState без результатов; NEED: —
[wishes-v2] feature/wishes — готово: редактирование по тапу на карточке, поиск + сброс, меню сортировки (новые/цена/алфавит) с галочкой, удаление с подтверждением, EmptyState для пустого поиска; NEED: —
[wishes] feature/wishes — готово: экран с hero-счётчиком, фильтрами, карточками, диалогом и FAB, WishesViewModel (add/setStatus/delete); NEED: viewModel { WishesViewModel(get(), get()) }
[main] core — добавлены Wish/Gift (домен, Room-таблицы v2, репозитории, DI); нужна только регистрация вьюмоделей в app.
[gifts-v2] feature/gifts — готово: правка по тапу на карточку (название, заметка, бюджет, статус), поиск по названию/заметке со сбросом, меню сортировки (новые/бюджет/алфавит) с галочкой, удаление с AlertDialog, EmptyState при пустом поиске; NEED: —
[gifts] feature/gifts — готово: ViewModel (список по статусам + суммы бюджета, add/setStatus/delete), экран с hero, секциями, карточками, диалогом добавления; NEED: viewModel { GiftsViewModel(get(), get()) }
[quizzes] feature/quizzes — готово: модуль core.quizzes (route `quizzes`, FUN, order 50, defaultEnabled), 3 теста по 7 вопросов в QuizContent, результат 0..100 с 3 градациями, DataStore `quiz_results` (последний процент + дата), экран список→вопросы→результат, @Preview; NEED: зависимость :feature:quizzes в app, регистрация QuizzesModule и viewModel
