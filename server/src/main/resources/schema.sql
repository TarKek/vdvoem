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