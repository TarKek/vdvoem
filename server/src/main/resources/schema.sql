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

-- Расширения желаний и подарков: таблицы уже существуют на рабочих базах,
-- поэтому колонки добавляются идемпотентно.
ALTER TABLE wishes ADD COLUMN IF NOT EXISTS priority INT NOT NULL DEFAULT 0;
ALTER TABLE wishes ADD COLUMN IF NOT EXISTS url TEXT NOT NULL DEFAULT '';
ALTER TABLE wishes ADD COLUMN IF NOT EXISTS surprise BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE wishes ADD COLUMN IF NOT EXISTS owner_device_id TEXT NOT NULL DEFAULT '';
ALTER TABLE gifts ADD COLUMN IF NOT EXISTS wish_id TEXT;

CREATE TABLE IF NOT EXISTS pokes (
    id         UUID PRIMARY KEY,
    pair_id    UUID NOT NULL REFERENCES pairs (id) ON DELETE CASCADE,
    text       TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    deleted_at BIGINT
);
CREATE INDEX IF NOT EXISTS idx_pokes_pair_updated ON pokes (pair_id, updated_at);

-- Результаты тестов пары: процент и время прохождения.
CREATE TABLE IF NOT EXISTS quiz_scores (
    id         UUID PRIMARY KEY,
    pair_id    UUID NOT NULL REFERENCES pairs (id) ON DELETE CASCADE,
    quiz_id    TEXT NOT NULL,
    percent    INT NOT NULL,
    passed_at  BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    deleted_at BIGINT
);
CREATE INDEX IF NOT EXISTS idx_quiz_scores_pair_updated ON quiz_scores (pair_id, updated_at);

-- Расписание уроков пары: день недели (1 = понедельник), время в минутах от полуночи.
CREATE TABLE IF NOT EXISTS lessons (
    id           UUID PRIMARY KEY,
    pair_id      UUID NOT NULL REFERENCES pairs (id) ON DELETE CASCADE,
    subject      TEXT NOT NULL DEFAULT '',
    weekday      INT NOT NULL,
    start_minute INT NOT NULL,
    end_minute   INT NOT NULL,
    room         TEXT NOT NULL DEFAULT '',
    teacher      TEXT NOT NULL DEFAULT '',
    updated_at   BIGINT NOT NULL,
    deleted_at   BIGINT
);
CREATE INDEX IF NOT EXISTS idx_lessons_pair_updated ON lessons (pair_id, updated_at);

-- Трекер менструального цикла пары: дни и настройки.
-- id здесь TEXT, а не UUID: если клиент принесёт id не в формате UUID (например, из
-- файла резервной копии), приведение типа не сорвёт весь push.
-- symptoms — TEXT[]: в JSON API это обычный массив строковых ключей.
CREATE TABLE IF NOT EXISTS cycle_days (
    id          TEXT PRIMARY KEY,
    pair_id     UUID NOT NULL REFERENCES pairs (id) ON DELETE CASCADE,
    date_millis BIGINT NOT NULL,
    period      BOOLEAN NOT NULL DEFAULT FALSE,
    flow        TEXT NOT NULL DEFAULT 'NONE',
    symptoms    TEXT[] NOT NULL DEFAULT '{}',
    mood        TEXT NOT NULL DEFAULT '',
    weight      REAL,
    note        TEXT NOT NULL DEFAULT '',
    updated_at  BIGINT NOT NULL,
    deleted_at  BIGINT
);

CREATE INDEX IF NOT EXISTS idx_cycle_days_pair_updated ON cycle_days (pair_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_cycle_days_pair_date ON cycle_days (pair_id, date_millis);

CREATE TABLE IF NOT EXISTS cycle_settings (
    id            TEXT PRIMARY KEY,
    pair_id       UUID NOT NULL REFERENCES pairs (id) ON DELETE CASCADE,
    cycle_length  INT NOT NULL DEFAULT 28,
    period_length INT NOT NULL DEFAULT 5,
    updated_at    BIGINT NOT NULL,
    deleted_at    BIGINT
);

CREATE INDEX IF NOT EXISTS idx_cycle_settings_pair_updated ON cycle_settings (pair_id, updated_at);

-- Монотонный курсор синхронизации. sync_cursor увеличивается в той же транзакции,
-- что и revision изменённых строк, поэтому гонки sequence/commit не теряют записи.
ALTER TABLE pairs ADD COLUMN IF NOT EXISTS sync_cursor BIGINT NOT NULL DEFAULT 0;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS device_key TEXT NOT NULL DEFAULT '';
ALTER TABLE pairs ADD COLUMN IF NOT EXISTS revision BIGINT NOT NULL DEFAULT 0;
ALTER TABLE events ADD COLUMN IF NOT EXISTS revision BIGINT NOT NULL DEFAULT 0;
ALTER TABLE wishes ADD COLUMN IF NOT EXISTS revision BIGINT NOT NULL DEFAULT 0;
ALTER TABLE gifts ADD COLUMN IF NOT EXISTS revision BIGINT NOT NULL DEFAULT 0;
ALTER TABLE gifts ADD COLUMN IF NOT EXISTS surprise BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE gifts ADD COLUMN IF NOT EXISTS owner_device_id TEXT NOT NULL DEFAULT '';
ALTER TABLE gifts ADD COLUMN IF NOT EXISTS bought_at BIGINT;
ALTER TABLE gifts ADD COLUMN IF NOT EXISTS given_at BIGINT;
ALTER TABLE pokes ADD COLUMN IF NOT EXISTS revision BIGINT NOT NULL DEFAULT 0;
ALTER TABLE quiz_scores ADD COLUMN IF NOT EXISTS revision BIGINT NOT NULL DEFAULT 0;
ALTER TABLE lessons ADD COLUMN IF NOT EXISTS revision BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cycle_days ADD COLUMN IF NOT EXISTS revision BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cycle_settings ADD COLUMN IF NOT EXISTS revision BIGINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_events_pair_revision ON events (pair_id, revision);
CREATE INDEX IF NOT EXISTS idx_wishes_pair_revision ON wishes (pair_id, revision);
CREATE INDEX IF NOT EXISTS idx_gifts_pair_revision ON gifts (pair_id, revision);
CREATE INDEX IF NOT EXISTS idx_pokes_pair_revision ON pokes (pair_id, revision);
CREATE INDEX IF NOT EXISTS idx_quiz_scores_pair_revision ON quiz_scores (pair_id, revision);
CREATE INDEX IF NOT EXISTS idx_lessons_pair_revision ON lessons (pair_id, revision);
CREATE INDEX IF NOT EXISTS idx_cycle_days_pair_revision ON cycle_days (pair_id, revision);
CREATE INDEX IF NOT EXISTS idx_cycle_settings_pair_revision ON cycle_settings (pair_id, revision);

-- Одноразовое наполнение для баз, созданных до серверных ревизий.
UPDATE pairs SET sync_cursor = GREATEST(sync_cursor, 1), revision = 1 WHERE revision = 0;
UPDATE events SET revision = 1 WHERE revision = 0;
UPDATE wishes SET revision = 1 WHERE revision = 0;
UPDATE gifts SET revision = 1 WHERE revision = 0;
UPDATE pokes SET revision = 1 WHERE revision = 0;
UPDATE quiz_scores SET revision = 1 WHERE revision = 0;
UPDATE lessons SET revision = 1 WHERE revision = 0;
UPDATE cycle_days SET revision = 1 WHERE revision = 0;
UPDATE cycle_settings SET revision = 1 WHERE revision = 0;

CREATE OR REPLACE FUNCTION vdvoem_assign_revision() RETURNS trigger AS $$
BEGIN
    SELECT sync_cursor INTO NEW.revision FROM pairs WHERE id = NEW.pair_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS assign_revision ON events;
CREATE TRIGGER assign_revision BEFORE INSERT OR UPDATE ON events
    FOR EACH ROW EXECUTE FUNCTION vdvoem_assign_revision();
DROP TRIGGER IF EXISTS assign_revision ON wishes;
CREATE TRIGGER assign_revision BEFORE INSERT OR UPDATE ON wishes
    FOR EACH ROW EXECUTE FUNCTION vdvoem_assign_revision();
DROP TRIGGER IF EXISTS assign_revision ON gifts;
CREATE TRIGGER assign_revision BEFORE INSERT OR UPDATE ON gifts
    FOR EACH ROW EXECUTE FUNCTION vdvoem_assign_revision();
DROP TRIGGER IF EXISTS assign_revision ON pokes;
CREATE TRIGGER assign_revision BEFORE INSERT OR UPDATE ON pokes
    FOR EACH ROW EXECUTE FUNCTION vdvoem_assign_revision();
DROP TRIGGER IF EXISTS assign_revision ON quiz_scores;
CREATE TRIGGER assign_revision BEFORE INSERT OR UPDATE ON quiz_scores
    FOR EACH ROW EXECUTE FUNCTION vdvoem_assign_revision();
DROP TRIGGER IF EXISTS assign_revision ON lessons;
CREATE TRIGGER assign_revision BEFORE INSERT OR UPDATE ON lessons
    FOR EACH ROW EXECUTE FUNCTION vdvoem_assign_revision();
DROP TRIGGER IF EXISTS assign_revision ON cycle_days;
CREATE TRIGGER assign_revision BEFORE INSERT OR UPDATE ON cycle_days
    FOR EACH ROW EXECUTE FUNCTION vdvoem_assign_revision();
DROP TRIGGER IF EXISTS assign_revision ON cycle_settings;
CREATE TRIGGER assign_revision BEFORE INSERT OR UPDATE ON cycle_settings
    FOR EACH ROW EXECUTE FUNCTION vdvoem_assign_revision();

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