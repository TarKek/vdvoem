# Backend «Вдвоём»

Kotlin + Ktor + PostgreSQL. Разворачивается на своём VPS через Docker Compose.

## Что уже есть

- `POST /v1/pairs` — создать пару, вернуть `pairId`, `deviceToken`, `inviteCode`.
- `POST /v1/pairs/join` — подключить второе устройство по коду приглашения.
- `GET /v1/sync?since=<millis>` — забрать изменения пары (pair, events, wishes, gifts).
- `POST /v1/sync` — отправить локальные изменения; конфликты решает last-write-wins по `updatedAt`,
  удаления — через `deletedAt` (tombstone).
- `GET /health` — проверка живости.

## Вложения и плагины (v2-задел)

- `POST /v1/attachments` — multipart upload (`file` + `entityType`, `entityId`, `kind`, `durationMs`,
  `width`, `height`); лимит 50 МБ, `pair_id` берётся из токена, файл: `attachments/<sha256>`
  (дедупликация по содержимому), ответ `201` с метаданными.
- `GET /v1/attachments?since=<millis>` — `{serverTime, attachments:[...]}`: метаданные пары без файлов,
  включая удалённые (`deletedAt`). `since` — `serverTime` предыдущего ответа.
- `GET /v1/attachments/{id}` — файл с `Content-Type`, поддержка `Range` (206) для видео.
- `DELETE /v1/attachments/{id}` — soft-delete (`deletedAt`), файл на диске остаётся.
- `GET /v1/plugins` — публичный каталог-массив (только не удалённые, `publishedAt` desc).
- `GET /v1/plugins/{id}/apk` — последний опубликованный APK.
- `POST /admin/plugins` — публикация: заголовок `X-Admin-Token` = `ADMIN_TOKEN` из `.env`
  (если переменная не задана — 503). multipart: `apk` + `id`, `version`, `title`,
  `description`, `category`, `minAppVersion`, `iconUrl`; файл: `plugins/<id>/<version>.apk`.

Файлы лежат в volume `storage_data` (`STORAGE_DIR=/data/storage`).

Авторизация — `Authorization: Bearer <deviceToken>` (в базе лежит только SHA-256 токена).

## Локальный запуск

```bash
cd server
docker compose up -d --build
curl http://127.0.0.1:8090/health
```

Пароль БД — переменная `DB_PASSWORD` в `server/.env` (файл не коммитится).
Токен публикации плагинов — `ADMIN_TOKEN` в том же `.env`; без него `POST /admin/plugins` отвечает 503.

## Прод (srv1)

- Каталог: `/opt/vdvoem`, API слушает `127.0.0.1:8090` и дополнительно публикуется наружу на `:18090`
  для совместимости со старыми APK. Production-адрес: `https://185.213.240.43.sslip.io:18443`.
  Caddy работает в host-network на `:80` (ACME) и `:18443` (HTTPS/WSS); занятый `:443/xray` не трогаем.
  Конфиг и сертификаты: `/opt/vdvoem/caddy`, контейнер `vdvoem-caddy`.
- Обновление: скопировать `server/` на сервер и выполнить `docker compose up -d --build`.
- Бэкапы: `docker compose exec -T db pg_dump -U vdvoem vdvoem > backup.sql` (по cron),
  файлы вложений/APK — из volume `storage_data` (`docker run --rm -v vdvoem_storage_data:/data ...`).

## Дальше по плану

1. WebSocket-канал для мгновенных обновлений.
2. Слой `SyncTransport` в Android: очередь изменений, ретраи, фоновый синк (WorkManager).
3. Привязка кода приглашения и смена устройства.