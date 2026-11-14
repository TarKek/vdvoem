# Backend «Вдвоём»

Kotlin + Ktor + PostgreSQL. Разворачивается на своём VPS через Docker Compose.

## Что уже есть

- `POST /v1/pairs` — создать пару, вернуть `pairId`, `deviceToken`, `inviteCode`.
- `POST /v1/pairs/join` — подключить второе устройство по коду приглашения.
- `GET /v1/sync?since=<millis>` — забрать изменения пары (pair, events, wishes, gifts).
- `POST /v1/sync` — отправить локальные изменения; конфликты решает last-write-wins по `updatedAt`,
  удаления — через `deletedAt` (tombstone).
- `GET /health` — проверка живости.

Авторизация — `Authorization: Bearer <deviceToken>` (в базе лежит только SHA-256 токена).

## Локальный запуск

```bash
cd server
docker compose up -d --build
curl http://127.0.0.1:8090/health
```

Пароль БД — переменная `DB_PASSWORD` в `server/.env` (файл не коммитится).

## Прод (srv1)

- Каталог: `/opt/vdvoem`, API слушает `127.0.0.1:8090`, наружу — через туннель.
- Обновление: скопировать `server/` на сервер и выполнить `docker compose up -d --build`.
- Бэкапы: `docker compose exec -T db pg_dump -U vdvoem vdvoem > backup.sql` (по cron).

## Дальше по плану

1. WebSocket-канал для мгновенных обновлений.
2. Слой `SyncTransport` в Android: очередь изменений, ретраи, фоновый синк (WorkManager).
3. Привязка кода приглашения и смена устройства.