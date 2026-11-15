# Синхронизация и расширения — контракт v2

Черновик API. Всё, что ниже, должно работать без переделки при добавлении новых
типов данных (фото, видео, голосовые) и плагинов.

## Принципы

- **Local-first.** Клиент всегда пишет в Room и работает офлайн. Синк — фоновая задача.
- **LWW по `updatedAt`**, удаление — `deletedAt` (tombstone), ID — UUID, генерирует клиент.
- **Всё расширяемое — через `entity_type` + `entity_id`**, а не отдельными таблицами на каждую фичу.
- Один аккаунт пары = один `pair_id`, устройства авторизуются токенами.

## Аутентификация

`Authorization: Bearer <deviceToken>` — токен выдаётся при создании/вступлении в пару.
В БД хранится только SHA-256 токена. Токен можно отозвать (удалить устройство).

## Пары

| Метод | Путь | Назначение |
|---|---|---|
| POST | `/v1/pairs` | создать пару → `{pairId, deviceToken, inviteCode}` |
| POST | `/v1/pairs/join` | войти по `inviteCode` → `{pairId, deviceToken, inviteCode}` |
| POST | `/v1/pairs/leave` | отозвать текущее устройство |

## Данные

| Метод | Путь | Назначение |
|---|---|---|
| GET | `/v1/sync?since=<ms>` | всё, что изменилось после `since` |
| POST | `/v1/sync` | отправить локальные изменения (LWW на сервере) |

`since` берётся из `serverTime` предыдущего ответа, а не из часов телефона.

### Формат изменений

```json
{
  "pair": {"id": "...", "nameA": "...", "nameB": "...", "sinceMillis": 0, "updatedAt": 0},
  "events": [{"id": "...", "title": "...", "dateMillis": 0, "annual": true, "updatedAt": 0}],
  "wishes": [{"id": "...", "title": "...", "status": "WANTED", "updatedAt": 0}],
  "gifts":  [{"id": "...", "title": "...", "status": "IDEA",  "updatedAt": 0}]
}
```

Новые сущности добавляются как новые ключи в этот объект. Клиент игнорирует неизвестные
ключи (`ignoreUnknownKeys = true`), сервер тоже — это и есть задел на «огромное количество функций».

## Вложения (фото, видео, голосовые) — задел v2

Метаданные синхронизируются как обычная сущность, а сам файл — отдельно, по требованию.

```sql
attachments(
  id UUID PK,
  pair_id UUID,
  entity_type TEXT,      -- 'event' | 'wish' | 'gift' | 'note' | ...
  entity_id UUID,
  kind TEXT,             -- 'photo' | 'video' | 'voice' | 'file'
  mime TEXT,
  size BIGINT,
  sha256 TEXT,
  storage_key TEXT,      -- путь в объектном хранилище
  duration_ms BIGINT,    -- для видео/голоса
  width INT, height INT, -- для фото/видео
  created_at BIGINT,
  updated_at BIGINT,
  deleted_at BIGINT
)
```

| Метод | Путь | Назначение |
|---|---|---|
| POST | `/v1/attachments` | multipart upload: `pair_id` из токена, поля `entityType/entityId/kind/durationMs/width/height`, файл `file` |
| GET | `/v1/attachments?since=<ms>` | список метаданных (без файлов) |
| GET | `/v1/attachments/{id}` | скачать файл (Range-запросы для видео) |
| DELETE | `/v1/attachments/{id}` | soft-delete |

Правила:
- файлы на диске/в S3 по `storage_key`, имя = `sha256` (дедупликация);
- клиент качает только те вложения, которые нужны на экране (lazy);
- локальная копия кешируется в `filesDir/attachments/<id>`;
- лимиты: 50 МБ на файл, тип проверяется по MIME + magic bytes.

## Плагины (дополнения) — задел v2

Каталог живёт на сервере, приложение скачивает только выбранное.

```sql
plugins(
  id TEXT PK,            -- 'core.music'
  title TEXT,
  description TEXT,
  category TEXT,
  version TEXT,          -- semver
  min_app_version INT,
  apk_sha256 TEXT,
  apk_size BIGINT,
  storage_key TEXT,
  icon_url TEXT,
  published_at BIGINT,
  deleted_at BIGINT
)
```

| Метод | Путь | Назначение |
|---|---|---|
| GET | `/v1/plugins` | каталог (публичный, кэшируется на клиенте) |
| GET | `/v1/plugins/{id}/apk` | скачать APK (только после подтверждения пользователя) |
| POST | `/admin/plugins` | публикация (заголовок `X-Admin-Token`, не для клиентов) |

Правила:
- каталог обновляется при открытии экрана «Дополнения», в фоне — раз в сутки;
- APK проверяется по `sha256` и подписи перед установкой;
- модуль остаётся выключенным, пока пользователь сам не включит.

## Ошибки

```json
{"error": "unauthorized"}
```

Коды: 400 — кривой запрос, 401 — нет/просрочен токен, 404 — не найдено,
409 — конфликт версий (клиент должен сделать pull и повторить), 413 — файл слишком большой.

## Хранение и бэкапы

- данные — PostgreSQL в Docker (`/opt/vdvoem`), файлы — volume на диске;
- ежедневный `pg_dump` + rsync файлов, хранить 14 дней;
- секреты — `.env` на сервере, в репозиторий не попадают.