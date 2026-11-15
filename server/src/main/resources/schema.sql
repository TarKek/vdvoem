-- Схема «Вдвоём» v1. Идентификаторы — UUID, синк по updatedAt/deletedAt.
CREATE TABLE IF NOT EXISTS pairs (
    id           UUID PRIMARY KEY,
    name_a       TEXT NOT NULL,
    name_b       TEXT NOT NULL,
    since_millis BIGINT NOT NULL,
    birthday_a   BIGINT,
    birthday_b   BIGINT,
    invite_code  TEXT NOT NULL UNIQUE,
    updated_at   BIGINT NOT NULL,
    deleted_at   BIGINT
);

CREATE TABLE IF NOT EXISTS devices (
    id         UUID PRIMARY KEY,
    pair_id    UUID NOT NULL REFERENCES pairs (id) ON DELETE CASCADE,
    token_hash TEXT NOT NULL UNIQUE,
    name       TEXT NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS events (
    id          UUID PRIMARY KEY,
    pair_id     UUID NOT NULL REFERENCES pairs (id) ON DELETE CASCADE,
    title       TEXT NOT NULL,
    date_millis BIGINT NOT NULL,
    annual      BOOLEAN NOT NULL,
    updated_at  BIGINT NOT NULL,
    deleted_at  BIGINT
);

CREATE TABLE IF NOT EXISTS wishes (
    id             UUID PRIMARY KEY,
    pair_id        UUID NOT NULL REFERENCES pairs (id) ON DELETE CASCADE,
    title          TEXT NOT NULL,
    note           TEXT NOT NULL DEFAULT '',
    price_estimate BIGINT,
    status         TEXT NOT NULL,
    updated_at     BIGINT NOT NULL,
    deleted_at     BIGINT
);

CREATE TABLE IF NOT EXISTS gifts (
    id         UUID PRIMARY KEY,
    pair_id    UUID NOT NULL REFERENCES pairs (id) ON DELETE CASCADE,
    title      TEXT NOT NULL,
    note       TEXT NOT NULL DEFAULT '',
    budget     BIGINT,
    status     TEXT NOT NULL,
    updated_at BIGINT NOT NULL,
    deleted_at BIGINT
);

CREATE INDEX IF NOT EXISTS idx_events_pair_updated ON events (pair_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_wishes_pair_updated ON wishes (pair_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_gifts_pair_updated ON gifts (pair_id, updated_at);

CREATE TABLE IF NOT EXISTS pokes (
    id         UUID PRIMARY KEY,
    pair_id    UUID NOT NULL REFERENCES pairs (id) ON DELETE CASCADE,
    text       TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    deleted_at BIGINT
);
CREATE INDEX IF NOT EXISTS idx_pokes_pair_updated ON pokes (pair_id, updated_at);

-- Вложения (фото, видео, голосовые): метаданные синкаются, файл лежит на диске по storage_key.
CREATE TABLE IF NOT EXISTS attachments (
    id          UUID PRIMARY KEY,
    pair_id     UUID NOT NULL REFERENCES pairs (id) ON DELETE CASCADE,
    entity_type TEXT NOT NULL,
    entity_id   UUID NOT NULL,
    kind        TEXT NOT NULL,
    mime        TEXT NOT NULL DEFAULT 'application/octet-stream',
    size        BIGINT NOT NULL DEFAULT 0,
    sha256      TEXT NOT NULL,
    storage_key TEXT NOT NULL,
    duration_ms BIGINT,
    width       INT,
    height      INT,
    created_at  BIGINT NOT NULL,
    updated_at  BIGINT NOT NULL,
    deleted_at  BIGINT
);

CREATE INDEX IF NOT EXISTS idx_attachments_pair_updated ON attachments (pair_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_attachments_entity ON attachments (entity_type, entity_id);

-- Каталог плагинов: одна строка на плагин, apk_* — последняя опубликованная версия.
CREATE TABLE IF NOT EXISTS plugins (
    id              TEXT PRIMARY KEY,
    title           TEXT NOT NULL,
    description     TEXT NOT NULL DEFAULT '',
    category        TEXT NOT NULL DEFAULT '',
    version         TEXT NOT NULL,
    min_app_version INT NOT NULL DEFAULT 0,
    apk_sha256      TEXT NOT NULL,
    apk_size        BIGINT NOT NULL DEFAULT 0,
    storage_key     TEXT NOT NULL,
    icon_url        TEXT NOT NULL DEFAULT '',
    published_at    BIGINT NOT NULL,
    deleted_at      BIGINT
);

CREATE INDEX IF NOT EXISTS idx_plugins_published ON plugins (published_at DESC);