# SDK плагинов «Вдвоём»

Плагин — это обычный Gradle-модуль `:feature:<имя>`, который реализует контракт `AppModule`.
Все плагины собираются в APK «по заводу»: их не нужно скачивать, у всех они есть.
Пользователь может только скрыть плагин из навигации, данные при этом остаются.

## Область (scope) — главное решение

У каждого плагина в манифесте есть поле `scope`:

| Scope | Что значит | Данные |
|---|---|---|
| `SHARED` | плагин для пары: календарь, желания, подарки, тесты, расписание | синхронизируются через сервер пары |
| `LOCAL` | плагин для одного человека: steam guard, заметки, музыка | лежат только на устройстве, в синк не попадают |

Это не «настройка», а часть контракта: пользователь видит, что общее, а что личное,
и не ждёт, что личные данные появятся у партнёра.

## Контракт

```kotlin
class MyModule : AppModule {
    override val manifest = ModuleManifest(
        id = "core.my",                       // уникальный, с префиксом автора/группы
        titleRes = R.string.my_module_title,  // только ресурсы, без литералов
        descriptionRes = R.string.my_module_description,
        category = ModuleCategory.FUN,        // CORE | TOGETHER | PERSONAL | FUN
        order = 75,                           // порядок по умолчанию; пользователь может менять
        defaultEnabled = true,                // всегда true: плагины включены по заводу
        scope = ModuleScope.SHARED            // SHARED или LOCAL
    )

    override val route = "my"                 // уникальный маршрут навигации
    override val icon: ImageVector = Icons.Filled.Star

    @Composable
    override fun Screen(modifier: Modifier) {
        val viewModel: MyViewModel = koinViewModel()
        MyScreen(state = ..., modifier = modifier)
    }
}

val myFeatureModule: Module = module {
    single { MyModule() }
    viewModel { MyViewModel(repository = get()) }
}
```

Регистрация — три места (делает мейнтейнер, но лучше сразу приложить патч):

1. `settings.gradle.kts` — `include(":feature:my")`
2. `app/build.gradle.kts` — `implementation(project(":feature:my"))`
3. `VdvoemApplication.kt` — `myFeatureModule` в списке `modules(...)`,
   `di/AppKoinModule.kt` — `get<MyModule>()` в реестре.

## Правила

**Обязательные**

- Ядро не знает о фичах: `:core:*` **не импортирует** `:feature:*`.
- Фичи не знают друг о друге. Нужен общий тип — он живёт в `:core:domain`.
- Все строки — в `values/strings.xml` **и** `values-en/strings.xml`, наборы ключей совпадают.
  Плейсхолдеры (`%1$s`, `%1$d`) в переводах сохраняются.
- Никаких хардкод-цветов, размеров и шрифтов: тема и токены из `:core:ui`, обёртки
  (`VdvoemCard`, `AppButton`, `EmptyState`, `SectionHeader`) вместо самодельных карточек.
- Каждый экран — с `@Preview`; итог проверяется на устройстве (светлая и тёмная тема).
- `LOCAL`-плагин **не** добавляет сущности в `core:data` и в синк: своя Room-база внутри модуля.

**Для `SHARED`-плагинов**

- Сущность домена: `UUID`, `pairId`, `updatedAt`, `deletedAt` (tombstone), запись только через репозиторий.
- Синхронизация: DTO в `core:sync/SyncDto.kt` с дефолтами для всех новых полей,
  мапперы туда-обратно, `applyXxx` (LWW по `updatedAt`), `collectLocalChanges`, таблица на сервере,
  идемпотентный `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`.
- Удаление — через `deletedAt`; без tombstone удаление не доедет до партнёра.
- Тесты: JVM-тест на правила LWW и на мапперы (см. `core/sync/src/test`).

## Чек-лист перед PR

- [ ] `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest` — зелёные
- [ ] `:feature:my:assembleDebug` собирается отдельно
- [ ] строки RU + EN, ключи совпадают
- [ ] `@Preview` на каждом новом экране
- [ ] `scope` выбран осознанно, для `LOCAL` нет обращений к синку и серверу
- [ ] нет импортов `:feature:*` из ядра и из других фич
- [ ] добавлены `NOTES.md` (строка в журнал) и, если нужно, раздел в `docs/PAIRING.md`

## Как предложить плагин

Плагины принимаются только через GitHub, рантайм-магазина нет:

1. **Идея** — issue с шаблоном «Новый плагин»: что делает, общий или личный, какие данные, экраны.
2. **Код** — PR из форка. В описании: id плагина, scope, что синкается, что проверено на устройстве.
3. **Ревью** — мейнтейнер проверяет чек-лист, собирает APK, смотрит на телефоне.

Шаблон модуля лежит в `plugin-template/` — скопируй папку, переименуй пакет и id.

## Что уже есть

| Плагин | id | Scope | Данные |
|---|---|---|---|
| Календарь | `core.calendar` | SHARED | события, напоминания |
| Желания и подарки | `presents` | SHARED | желания, подарки |
| Тесты | `core.quizzes` | SHARED | результаты |
| Расписание | `core.timetable` | SHARED | уроки |
| Заметки | `core.notes` | LOCAL | заметки |
| Настройки | `core.settings` | CORE | только локальные настройки |