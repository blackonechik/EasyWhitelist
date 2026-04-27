-- Schema for the external website and for manual DB management.
-- The plugin also creates this table automatically if it does not exist.

CREATE TABLE IF NOT EXISTS whitelist_entries (
    nickname VARCHAR(16) PRIMARY KEY,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    purchase_id BIGINT,
    source VARCHAR(32)
);

-- Add or activate a nickname.
-- Use lowercase nicknames to match the plugin's normalization.
INSERT INTO whitelist_entries (nickname, active, updated_at)
VALUES ('exampleplayer', TRUE, NOW())
ON CONFLICT (nickname) DO UPDATE
SET active = EXCLUDED.active,
    updated_at = NOW();

-- Deactivate a nickname without deleting history.
UPDATE whitelist_entries
SET active = FALSE,
    updated_at = NOW()
WHERE nickname = 'exampleplayer';

-- Remove a nickname entirely.
DELETE FROM whitelist_entries
WHERE nickname = 'exampleplayer';

-- Read-only check for your backend.
SELECT nickname, active, created_at, updated_at
FROM whitelist_entries
WHERE nickname = 'exampleplayer';