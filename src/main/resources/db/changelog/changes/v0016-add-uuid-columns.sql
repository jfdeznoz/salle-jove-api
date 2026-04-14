--liquibase formatted sql

--changeset sallejoven:v0016a-add-uuid-columns splitStatements:true endDelimiter:;
CREATE OR REPLACE FUNCTION sallejoven_backfill_uuid()
RETURNS uuid
LANGUAGE SQL
AS $$
  SELECT md5(
      random()::text ||
      clock_timestamp()::text ||
      txid_current()::text
  )::uuid
$$;

-- Re-sincroniza secuencias SERIAL/IDENTITY con el MAX(id) real antes de tocar
-- datos, para evitar colisiones en altas concurrentes después del despliegue.
SELECT setval(
    pg_get_serial_sequence('user_group', 'id'),
    COALESCE((SELECT MAX(id) FROM user_group), 0) + 1,
    false
)
WHERE pg_get_serial_sequence('user_group', 'id') IS NOT NULL;

SELECT setval(
    pg_get_serial_sequence('event_group', 'id'),
    COALESCE((SELECT MAX(id) FROM event_group), 0) + 1,
    false
)
WHERE pg_get_serial_sequence('event_group', 'id') IS NOT NULL;

SELECT setval(
    pg_get_serial_sequence('event_user', 'id'),
    COALESCE((SELECT MAX(id) FROM event_user), 0) + 1,
    false
)
WHERE pg_get_serial_sequence('event_user', 'id') IS NOT NULL;

SELECT setval(
    pg_get_serial_sequence('weekly_session_user', 'id'),
    COALESCE((SELECT MAX(id) FROM weekly_session_user), 0) + 1,
    false
)
WHERE pg_get_serial_sequence('weekly_session_user', 'id') IS NOT NULL;

ALTER TABLE center ADD COLUMN IF NOT EXISTS uuid UUID;
UPDATE center SET uuid = sallejoven_backfill_uuid() WHERE uuid IS NULL;
ALTER TABLE center ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_center_uuid ON center(uuid);

ALTER TABLE group_salle ADD COLUMN IF NOT EXISTS uuid UUID;
UPDATE group_salle SET uuid = sallejoven_backfill_uuid() WHERE uuid IS NULL;
ALTER TABLE group_salle ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_group_salle_uuid ON group_salle(uuid);

ALTER TABLE user_salle ADD COLUMN IF NOT EXISTS uuid UUID;
UPDATE user_salle SET uuid = sallejoven_backfill_uuid() WHERE uuid IS NULL;
ALTER TABLE user_salle ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_salle_uuid ON user_salle(uuid);

ALTER TABLE user_group ADD COLUMN IF NOT EXISTS uuid UUID;
UPDATE user_group SET uuid = sallejoven_backfill_uuid() WHERE uuid IS NULL;
ALTER TABLE user_group ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_group_uuid ON user_group(uuid);

ALTER TABLE event ADD COLUMN IF NOT EXISTS uuid UUID;
UPDATE event SET uuid = sallejoven_backfill_uuid() WHERE uuid IS NULL;
ALTER TABLE event ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_event_uuid ON event(uuid);

ALTER TABLE event_group ADD COLUMN IF NOT EXISTS uuid UUID;
UPDATE event_group SET uuid = sallejoven_backfill_uuid() WHERE uuid IS NULL;
ALTER TABLE event_group ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_event_group_uuid ON event_group(uuid);

ALTER TABLE event_user ADD COLUMN IF NOT EXISTS uuid UUID;
UPDATE event_user SET uuid = sallejoven_backfill_uuid() WHERE uuid IS NULL;
ALTER TABLE event_user ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_event_user_uuid ON event_user(uuid);

ALTER TABLE refresh_token ADD COLUMN IF NOT EXISTS uuid UUID;
UPDATE refresh_token SET uuid = sallejoven_backfill_uuid() WHERE uuid IS NULL;
ALTER TABLE refresh_token ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_refresh_token_uuid ON refresh_token(uuid);

ALTER TABLE password_reset_tokens ADD COLUMN IF NOT EXISTS uuid UUID;
UPDATE password_reset_tokens SET uuid = sallejoven_backfill_uuid() WHERE uuid IS NULL;
ALTER TABLE password_reset_tokens ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_password_reset_token_uuid ON password_reset_tokens(uuid);

ALTER TABLE historical_data ADD COLUMN IF NOT EXISTS uuid UUID;
UPDATE historical_data SET uuid = sallejoven_backfill_uuid() WHERE uuid IS NULL;
ALTER TABLE historical_data ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_historical_data_uuid ON historical_data(uuid);

ALTER TABLE user_pending ADD COLUMN IF NOT EXISTS uuid UUID;
UPDATE user_pending SET uuid = sallejoven_backfill_uuid() WHERE uuid IS NULL;
ALTER TABLE user_pending ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_pending_uuid ON user_pending(uuid);

ALTER TABLE user_pending ADD COLUMN IF NOT EXISTS center_uuid UUID;
ALTER TABLE user_pending ADD COLUMN IF NOT EXISTS group_uuid UUID;

UPDATE user_pending up
SET center_uuid = c.uuid
FROM center c
WHERE up.center_uuid IS NULL
  AND up.center_id IS NOT NULL
  AND c.id = up.center_id;

UPDATE user_pending up
SET group_uuid = g.uuid
FROM group_salle g
WHERE up.group_uuid IS NULL
  AND up.group_id IS NOT NULL
  AND g.id = up.group_id;

CREATE INDEX IF NOT EXISTS idx_user_pending_center_uuid ON user_pending(center_uuid);
CREATE INDEX IF NOT EXISTS idx_user_pending_group_uuid ON user_pending(group_uuid);

ALTER TABLE user_center ADD COLUMN IF NOT EXISTS uuid UUID;
UPDATE user_center SET uuid = sallejoven_backfill_uuid() WHERE uuid IS NULL;
ALTER TABLE user_center ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_center_uuid ON user_center(uuid);

ALTER TABLE vital_situation ADD COLUMN IF NOT EXISTS uuid UUID;
UPDATE vital_situation SET uuid = sallejoven_backfill_uuid() WHERE uuid IS NULL;
ALTER TABLE vital_situation ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_vital_situation_uuid ON vital_situation(uuid);

ALTER TABLE vital_situation_session ADD COLUMN IF NOT EXISTS uuid UUID;
UPDATE vital_situation_session SET uuid = sallejoven_backfill_uuid() WHERE uuid IS NULL;
ALTER TABLE vital_situation_session ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_vital_situation_session_uuid ON vital_situation_session(uuid);

ALTER TABLE weekly_session ADD COLUMN IF NOT EXISTS uuid UUID;
UPDATE weekly_session SET uuid = sallejoven_backfill_uuid() WHERE uuid IS NULL;
ALTER TABLE weekly_session ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_weekly_session_uuid ON weekly_session(uuid);

ALTER TABLE weekly_session_user ADD COLUMN IF NOT EXISTS uuid UUID;
UPDATE weekly_session_user SET uuid = sallejoven_backfill_uuid() WHERE uuid IS NULL;
ALTER TABLE weekly_session_user ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_weekly_session_user_uuid ON weekly_session_user(uuid);
