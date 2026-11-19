# Модуль «Цикл» — контракт для разработки

Трекер менструального цикла для пары: общие данные, оба партнёра могут редактировать.
Первый релиз — «почти как Flo»: календарь, день цикла, прогноз, симптомы, настроение, вес,
история циклов, график длины цикла, советы на день.

## 1. Границы модулей

| Слой | Где живёт | Кто пишет |
|---|---|---|
| Модель, репозиторий, прогноз | `core:domain` (`app.vdvoem.core.domain.cycle`) | агент А |
| Room, DTO, синк, сервер | `core:data`, `core:sync`, `server` | агент А |
| Экран | `feature/cycle` (`app.vdvoem.feature.cycle`) | агент Б |
| Push/WS | `core:sync`, `server`, `app/.../push` | агент В |

Модуль: `ModuleManifest(id = "core.cycle", route = "cycle", category = ModuleCategory.TOGETHER,
scope = ModuleScope.SHARED, order = 45, defaultEnabled = true)`, иконка `Icons.Filled.Favorite`.

## 2. Доменная модель (`core/domain/.../model/Cycle.kt`)

```kotlin
enum class Flow { NONE, LIGHT, MEDIUM, HEAVY }         // NONE = месячных нет

data class CycleDay(
    val id: String,              // UUID записи
    val pairId: String,
    val dateMillis: Long,        // начало дня в UTC (как CalendarEvent.dateMillis)
    val period: Boolean = false, // день месячных
    val flow: Flow = Flow.NONE,
    val symptoms: List<String> = emptyList(),  // ключи: "cramps","headache","backache","acne","bloating","fatigue","nausea","breasts","sleep"
    val mood: String = "",       // ключ: "happy","calm","sad","irritable","anxious","energetic"
    val weight: Float? = null,   // кг, одна десятая
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

data class CycleSettings(
    val id: String,              // = pairId
    val pairId: String,
    val cycleLength: Int = 28,   // ожидаемая длина цикла, дней
    val periodLength: Int = 5,   // ожидаемая длина месячных
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
```

Ключи `symptoms` и `mood` — стабильные строки (не локализованный текст): подписи лежат в
`feature/cycle/src/main/res/values/strings.xml` и `values-en/strings.xml`.

## 3. Прогноз (`core/domain/.../cycle/CycleForecast.kt`), чистый Kotlin

```kotlin
enum class CyclePhase { PERIOD, FOLLICULAR, OVULATION, LUTEAL }

data class CycleStats(
    val currentDay: Int,            // 1..N, день текущего цикла
    val phase: CyclePhase,
    val lastPeriodStart: LocalDate?,
    val nextPeriodStart: LocalDate?,
    val daysUntilNextPeriod: Long?,
    val ovulationDate: LocalDate?,
    val fertileStart: LocalDate?,   // окно ±2 дня от овуляции
    val fertileEnd: LocalDate?,
    val lengths: List<Int>,         // история длин завершённых циклов, старые → новые
    val averageLength: Int,
    val shortestLength: Int,
    val longestLength: Int,
    val periodLength: Int,
    val isLate: Boolean             // текущий цикл длиннее средней + 3 дня
)

fun buildForecast(days: List<CycleDay>, settings: CycleSettings?, today: LocalDate = Dates.today()): CycleStats
```

Правила: начало цикла — день с `period = true`, у которого предыдущий день не был `period`
(или разрыв > 1 дня). Овуляция — за 14 дней до следующего начала. Средняя длина — среднее
по завершённым циклам, если их меньше двух — `settings.cycleLength`. Пустых дней и удалённых
(`deletedAt != null`) не учитывать.

## 4. Репозиторий (`core/domain/.../repository/CycleRepository.kt`)

```kotlin
interface CycleRepository {
    fun observeDays(): Flow<List<CycleDay>>                                  // без tombstone
    fun observeDaysIncludingDeleted(): Flow<List<CycleDay>>                  // для синка
    fun observeSettings(): Flow<CycleSettings?>
    fun observeSettingsIncludingDeleted(): Flow<List<CycleSettings>>
    suspend fun saveDay(day: CycleDay)
    suspend fun saveSettings(settings: CycleSettings)
}
```

Room: таблицы `cycle_days` и `cycle_settings`, база версии 8, `MIGRATION_7_8`
(создание обеих таблиц, дефолты колонок как в entity). Реализация — `RoomCycleRepository`
с `LocalChangeSignal.notifyLocalWrite()` после записи. Регистрация в `DataModule`.

## 5. Синк

`CycleDayDto(id, dateMillis, period, flow, symptoms, mood, weight, note, updatedAt, deletedAt, pairId)`
и `CycleSettingsDto(id, cycleLength, periodLength, updatedAt, deletedAt, pairId)` в `ChangesDto`
(`cycleDays`, `cycleSettings`), участвуют в `size`, `collectLocalChanges`, `applyCycleDays`,
`applyCycleSettings` (LWW через `shouldApplyRemote`), `maxUpdatedAt`.

Сервер: таблицы `cycle_days`, `cycle_settings` (идемпотентный `CREATE TABLE IF NOT EXISTS` +
индексы), pull `updated_at > since`, push `ON CONFLICT DO UPDATE ... WHERE EXCLUDED.updated_at >`.
Массивы `symptoms` — `TEXT[]` (или JSON-строка; выбери одно и задокументируй).

## 6. Экран (`feature/cycle`)

Структура (всё в одном `CycleScreen.kt` + `CycleViewModel.kt`):
1. Hero-карточка: «Месячные: N-й день» или «До месячных N дней», фаза, длина предыдущего цикла,
   подпись «Цикл длился N дней».
2. Кнопки-действия: «Изменить месячные» (шторка: начался сегодня / закончился / выбор даты
   через DatePicker), «Симптомы» (шторка: чипы симптомов, настроение, вес, заметка), «Секс» —
   не нужен, вместо него «Заметка».
3. Мини-календарь месяца: дни месячных залиты, прогноз — пунктиром, овуляция — акцент,
   сегодня — обводка; тап по дню открывает шторку дня.
4. «История циклов»: список завершённых циклов (длина, даты), «Текущий цикл: N день».
5. «Динамика цикла»: простой график длина по циклам на Canvas (без сторонних библиотек),
   под ним average/shortest/longest.
6. «Советы на день»: 3–5 карточек по фазе (тексты в string-ресурсах, без внешних картинок).
7. Empty state: «Отметьте начало месячных» + подписано, что данные общие с партнёром.

Правила: только `VdvoemTheme`/токены и обёртки `:core:ui`, цвета — из темы, тексты — в
`values/strings.xml` + `values-en/strings.xml` (одинаковые наборы ключей), `@Preview` минимум
для hero и календаря.

## 7. Виджеты

- В приложении: новый тип виджета «Главной» `cycle` (`WIDGET_CYCLE` в `HomePreferences`,
  каталог `widgetCatalog`, карточка «Месячные · N-й день» / «До месячных N»).
- Системный: `app/src/main/java/app/vdvoem/widget/CycleWidgetProvider.kt` через Glance
  (`androidx.glance:glance-appwidget:1.1.1`), обновление раз в сутки + после синка.

## 8. Push «в ту же секунду»

Сервер: WebSocket `/v1/ws` с `Authorization: Bearer <deviceToken>`; при `POST /v1/sync`
сервер рассылает остальным устройствам пары `{"type":"changed","serverTime":...}`.
Клиент: `core:sync` — `PushClient` (Ktor WebSocket, реконнект с backoff), `app/.../push/PushService`
(foreground service, постоянное уведомление «Вдвоём: синхронизация»), на событие — `syncNow()`.
12-секундный опрос остаётся как fallback при отсутствии соединения.

Доставка не должна зависеть от того, открыл ли человек приложение. Поэтому у канала
четыре независимых слоя:

1. **Пинг сокета** (20 с, OkHttp) — мобильные операторы и роутеры молча выбрасывают
   простаивающий TCP через 3–10 минут; без пинга клиент ждал кадр, которого уже не будет,
   и уведомления появлялись только при следующем открытии приложения.
2. **Смена сети** (`ConnectivityManager`) — пересоздаёт сокет: старый TCP при переходе
   Wi-Fi → LTE мёртв, а цикл об этом ещё не знает.
3. **Сторожевой синк** раз в 5 минут в `PushService` — страховка от «тихой смерти» сокета,
   которую пинг не заметил.
4. **Возврат сервиса к жизни** без участия человека: `PushRestartReceiver` на
   `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED` и на будильник после свайпа из «недавних»;
   `START_STICKY` на случай, если систему процесс всё-таки выгрузила.

Тип foreground-сервиса `specialUse` (а не только `dataSync`): у `dataSync` на Android 15
есть лимит 6 часов, после которого система гасит сервис и зовёт `Service.onTimeout`.

Последнее звено — сама прошивка: Samsung/Xiaomi усыпляют приложения и режут сеть в Doze.
Поэтому на экране «Синхронизация пары» есть карточка «Уведомления в фоне» с запросом
`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: без исключения из оптимизации батареи ни один
из слоёв выше не гарантирует мгновенную доставку.