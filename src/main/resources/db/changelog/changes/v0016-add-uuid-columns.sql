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

--changeset sallejoven:v0016b-hard-switch-children splitStatements:false
-- ============================================================
-- HARD SWITCH A UUID COMO PK/FK CANONICA
-- ============================================================
-- Transaccion independiente para que los UPDATEs que referencian tablas padre
-- via sus _id antiguos no mantengan locks ni plan cache activos cuando el
-- siguiente bloque haga el DROP del id del padre.

-- ---------- group_salle.center -> center_uuid ----------
ALTER TABLE group_salle ADD COLUMN IF NOT EXISTS center_uuid UUID;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_name = 'group_salle' AND column_name = 'center'
  ) THEN
    UPDATE group_salle gs
    SET center_uuid = c.uuid
    FROM center c
    WHERE gs.center_uuid IS NULL
      AND gs.center IS NOT NULL
      AND c.id = gs.center;

    IF EXISTS (
      SELECT 1
      FROM group_salle
      WHERE center IS NOT NULL
        AND center_uuid IS NULL
    ) THEN
      RAISE EXCEPTION 'v0016 hard switch: group_salle.center_uuid no pudo rellenarse';
    END IF;
  END IF;
END $$;

DO $$
DECLARE
  rec RECORD;
BEGIN
  FOR rec IN
    SELECT con.conname
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY (con.conkey)
    WHERE rel.relname = 'group_salle'
      AND con.contype = 'f'
      AND att.attname = 'center'
  LOOP
    EXECUTE format('ALTER TABLE group_salle DROP CONSTRAINT IF EXISTS %I', rec.conname);
  END LOOP;
END $$;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_name = 'group_salle' AND column_name = 'center_uuid'
  ) THEN
    ALTER TABLE group_salle ALTER COLUMN center_uuid SET NOT NULL;
  END IF;
END $$;

ALTER TABLE group_salle DROP COLUMN IF EXISTS center;
CREATE INDEX IF NOT EXISTS idx_group_salle_center_uuid ON group_salle(center_uuid);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conrelid = 'group_salle'::regclass
      AND conname = 'fk_group_salle_center'
  ) THEN
    ALTER TABLE group_salle
      ADD CONSTRAINT fk_group_salle_center
      FOREIGN KEY (center_uuid) REFERENCES center(uuid);
  END IF;
END $$;

-- ---------- user_group(user_salle, group_salle) -> (user_uuid, group_uuid) ----------
ALTER TABLE user_group ADD COLUMN IF NOT EXISTS user_uuid UUID;
ALTER TABLE user_group ADD COLUMN IF NOT EXISTS group_uuid UUID;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'user_group' AND column_name = 'user_salle'
  ) THEN
    UPDATE user_group ug
    SET user_uuid = u.uuid
    FROM user_salle u
    WHERE ug.user_uuid IS NULL
      AND ug.user_salle IS NOT NULL
      AND u.id = ug.user_salle;

    IF EXISTS (
      SELECT 1 FROM user_group WHERE user_salle IS NOT NULL AND user_uuid IS NULL
    ) THEN
      RAISE EXCEPTION 'v0016 hard switch: user_group.user_uuid no pudo rellenarse';
    END IF;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'user_group' AND column_name = 'group_salle'
  ) THEN
    UPDATE user_group ug
    SET group_uuid = g.uuid
    FROM group_salle g
    WHERE ug.group_uuid IS NULL
      AND ug.group_salle IS NOT NULL
      AND g.id = ug.group_salle;

    IF EXISTS (
      SELECT 1 FROM user_group WHERE group_salle IS NOT NULL AND group_uuid IS NULL
    ) THEN
      RAISE EXCEPTION 'v0016 hard switch: user_group.group_uuid no pudo rellenarse';
    END IF;
  END IF;
END $$;

DO $$
DECLARE
  rec RECORD;
BEGIN
  FOR rec IN
    SELECT con.conname
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY (con.conkey)
    WHERE rel.relname = 'user_group'
      AND con.contype = 'f'
      AND att.attname IN ('user_salle', 'group_salle')
  LOOP
    EXECUTE format('ALTER TABLE user_group DROP CONSTRAINT IF EXISTS %I', rec.conname);
  END LOOP;
END $$;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'user_group' AND column_name = 'user_uuid') THEN
    ALTER TABLE user_group ALTER COLUMN user_uuid SET NOT NULL;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'user_group' AND column_name = 'group_uuid') THEN
    ALTER TABLE user_group ALTER COLUMN group_uuid SET NOT NULL;
  END IF;
END $$;

ALTER TABLE user_group DROP COLUMN IF EXISTS user_salle;
ALTER TABLE user_group DROP COLUMN IF EXISTS group_salle;

CREATE INDEX IF NOT EXISTS idx_user_group_user_uuid ON user_group(user_uuid);
CREATE INDEX IF NOT EXISTS idx_user_group_group_uuid ON user_group(group_uuid);

DROP INDEX IF EXISTS uq_user_group_user_group_year;
DROP INDEX IF EXISTS uq_user_group_user_group_year_active;
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_group_user_group_year_active
  ON user_group(user_uuid, group_uuid, year)
  WHERE deleted_at IS NULL;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conrelid = 'user_group'::regclass
      AND conname = 'fk_user_group_user'
  ) THEN
    ALTER TABLE user_group
      ADD CONSTRAINT fk_user_group_user
      FOREIGN KEY (user_uuid) REFERENCES user_salle(uuid);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conrelid = 'user_group'::regclass
      AND conname = 'fk_user_group_group'
  ) THEN
    ALTER TABLE user_group
      ADD CONSTRAINT fk_user_group_group
      FOREIGN KEY (group_uuid) REFERENCES group_salle(uuid);
  END IF;
END $$;

-- ---------- user_center(user_salle, center) -> (user_uuid, center_uuid) ----------
ALTER TABLE user_center ADD COLUMN IF NOT EXISTS user_uuid UUID;
ALTER TABLE user_center ADD COLUMN IF NOT EXISTS center_uuid UUID;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'user_center' AND column_name = 'user_salle'
  ) THEN
    UPDATE user_center uc
    SET user_uuid = u.uuid
    FROM user_salle u
    WHERE uc.user_uuid IS NULL
      AND uc.user_salle IS NOT NULL
      AND u.id = uc.user_salle;

    IF EXISTS (
      SELECT 1 FROM user_center WHERE user_salle IS NOT NULL AND user_uuid IS NULL
    ) THEN
      RAISE EXCEPTION 'v0016 hard switch: user_center.user_uuid no pudo rellenarse';
    END IF;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'user_center' AND column_name = 'center'
  ) THEN
    UPDATE user_center uc
    SET center_uuid = c.uuid
    FROM center c
    WHERE uc.center_uuid IS NULL
      AND uc.center IS NOT NULL
      AND c.id = uc.center;

    IF EXISTS (
      SELECT 1 FROM user_center WHERE center IS NOT NULL AND center_uuid IS NULL
    ) THEN
      RAISE EXCEPTION 'v0016 hard switch: user_center.center_uuid no pudo rellenarse';
    END IF;
  END IF;
END $$;

DO $$
DECLARE
  rec RECORD;
BEGIN
  FOR rec IN
    SELECT con.conname
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY (con.conkey)
    WHERE rel.relname = 'user_center'
      AND con.contype = 'f'
      AND att.attname IN ('user_salle', 'center')
  LOOP
    EXECUTE format('ALTER TABLE user_center DROP CONSTRAINT IF EXISTS %I', rec.conname);
  END LOOP;
END $$;

DO $$
BEGIN
  ALTER TABLE user_center ALTER COLUMN user_uuid SET NOT NULL;
  ALTER TABLE user_center ALTER COLUMN center_uuid SET NOT NULL;
END $$;

ALTER TABLE user_center DROP COLUMN IF EXISTS user_salle;
ALTER TABLE user_center DROP COLUMN IF EXISTS center;

CREATE INDEX IF NOT EXISTS idx_user_center_user_uuid ON user_center(user_uuid) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_user_center_center_uuid ON user_center(center_uuid) WHERE deleted_at IS NULL;

DROP INDEX IF EXISTS uq_user_center_active;
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_center_active
  ON user_center(user_uuid, center_uuid, year, user_type)
  WHERE deleted_at IS NULL;

DROP INDEX IF EXISTS idx_user_center_user_year;
CREATE INDEX IF NOT EXISTS idx_user_center_user_year
  ON user_center(user_uuid, year)
  WHERE deleted_at IS NULL;

DROP INDEX IF EXISTS idx_user_center_center_year;
CREATE INDEX IF NOT EXISTS idx_user_center_center_year
  ON user_center(center_uuid, year)
  WHERE deleted_at IS NULL;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conrelid = 'user_center'::regclass
      AND conname = 'fk_user_center_user'
  ) THEN
    ALTER TABLE user_center
      ADD CONSTRAINT fk_user_center_user
      FOREIGN KEY (user_uuid) REFERENCES user_salle(uuid);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conrelid = 'user_center'::regclass
      AND conname = 'fk_user_center_center'
  ) THEN
    ALTER TABLE user_center
      ADD CONSTRAINT fk_user_center_center
      FOREIGN KEY (center_uuid) REFERENCES center(uuid);
  END IF;
END $$;

-- ---------- vital_situation_session.vital_situation_id -> vital_situation_uuid ----------
ALTER TABLE vital_situation_session ADD COLUMN IF NOT EXISTS vital_situation_uuid UUID;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'vital_situation_session' AND column_name = 'vital_situation_id'
  ) THEN
    UPDATE vital_situation_session vss
    SET vital_situation_uuid = vs.uuid
    FROM vital_situation vs
    WHERE vss.vital_situation_uuid IS NULL
      AND vss.vital_situation_id IS NOT NULL
      AND vs.id = vss.vital_situation_id;

    IF EXISTS (
      SELECT 1 FROM vital_situation_session
      WHERE vital_situation_id IS NOT NULL
        AND vital_situation_uuid IS NULL
    ) THEN
      RAISE EXCEPTION 'v0016 hard switch: vital_situation_session.vital_situation_uuid no pudo rellenarse';
    END IF;
  END IF;
END $$;

DO $$
DECLARE
  rec RECORD;
BEGIN
  FOR rec IN
    SELECT con.conname
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY (con.conkey)
    WHERE rel.relname = 'vital_situation_session'
      AND con.contype = 'f'
      AND att.attname = 'vital_situation_id'
  LOOP
    EXECUTE format('ALTER TABLE vital_situation_session DROP CONSTRAINT IF EXISTS %I', rec.conname);
  END LOOP;
END $$;

ALTER TABLE vital_situation_session ALTER COLUMN vital_situation_uuid SET NOT NULL;
ALTER TABLE vital_situation_session DROP COLUMN IF EXISTS vital_situation_id;
CREATE INDEX IF NOT EXISTS idx_vital_situation_session_vital_situation_uuid ON vital_situation_session(vital_situation_uuid);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conrelid = 'vital_situation_session'::regclass
      AND conname = 'fk_vital_situation_session_vital_situation'
  ) THEN
    ALTER TABLE vital_situation_session
      ADD CONSTRAINT fk_vital_situation_session_vital_situation
      FOREIGN KEY (vital_situation_uuid) REFERENCES vital_situation(uuid);
  END IF;
END $$;

-- ---------- weekly_session(vital_situation_session_id, group_salle_id) -> (..._uuid, group_uuid) ----------
ALTER TABLE weekly_session ADD COLUMN IF NOT EXISTS vital_situation_session_uuid UUID;
ALTER TABLE weekly_session ADD COLUMN IF NOT EXISTS group_uuid UUID;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'weekly_session' AND column_name = 'vital_situation_session_id'
  ) THEN
    UPDATE weekly_session ws
    SET vital_situation_session_uuid = vss.uuid
    FROM vital_situation_session vss
    WHERE ws.vital_situation_session_uuid IS NULL
      AND ws.vital_situation_session_id IS NOT NULL
      AND vss.id = ws.vital_situation_session_id;

    IF EXISTS (
      SELECT 1 FROM weekly_session
      WHERE vital_situation_session_id IS NOT NULL
        AND vital_situation_session_uuid IS NULL
    ) THEN
      RAISE EXCEPTION 'v0016 hard switch: weekly_session.vital_situation_session_uuid no pudo rellenarse';
    END IF;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'weekly_session' AND column_name = 'group_salle_id'
  ) THEN
    UPDATE weekly_session ws
    SET group_uuid = g.uuid
    FROM group_salle g
    WHERE ws.group_uuid IS NULL
      AND ws.group_salle_id IS NOT NULL
      AND g.id = ws.group_salle_id;

    IF EXISTS (
      SELECT 1 FROM weekly_session
      WHERE group_salle_id IS NOT NULL
        AND group_uuid IS NULL
    ) THEN
      RAISE EXCEPTION 'v0016 hard switch: weekly_session.group_uuid no pudo rellenarse';
    END IF;
  END IF;
END $$;

DO $$
DECLARE
  rec RECORD;
BEGIN
  FOR rec IN
    SELECT con.conname
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY (con.conkey)
    WHERE rel.relname = 'weekly_session'
      AND con.contype = 'f'
      AND att.attname IN ('vital_situation_session_id', 'group_salle_id')
  LOOP
    EXECUTE format('ALTER TABLE weekly_session DROP CONSTRAINT IF EXISTS %I', rec.conname);
  END LOOP;
END $$;

ALTER TABLE weekly_session ALTER COLUMN vital_situation_session_uuid SET NOT NULL;
ALTER TABLE weekly_session ALTER COLUMN group_uuid SET NOT NULL;
ALTER TABLE weekly_session DROP COLUMN IF EXISTS vital_situation_session_id;
ALTER TABLE weekly_session DROP COLUMN IF EXISTS group_salle_id;

CREATE INDEX IF NOT EXISTS idx_weekly_session_vital_situation_session_uuid ON weekly_session(vital_situation_session_uuid);
CREATE INDEX IF NOT EXISTS idx_weekly_session_group_uuid ON weekly_session(group_uuid);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conrelid = 'weekly_session'::regclass
      AND conname = 'fk_weekly_session_vital_situation_session'
  ) THEN
    ALTER TABLE weekly_session
      ADD CONSTRAINT fk_weekly_session_vital_situation_session
      FOREIGN KEY (vital_situation_session_uuid) REFERENCES vital_situation_session(uuid);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conrelid = 'weekly_session'::regclass
      AND conname = 'fk_weekly_session_group'
  ) THEN
    ALTER TABLE weekly_session
      ADD CONSTRAINT fk_weekly_session_group
      FOREIGN KEY (group_uuid) REFERENCES group_salle(uuid);
  END IF;
END $$;

-- ---------- event_group(event, group_salle) -> (event_uuid, group_uuid) ----------
ALTER TABLE event_group ADD COLUMN IF NOT EXISTS event_uuid UUID;
ALTER TABLE event_group ADD COLUMN IF NOT EXISTS group_uuid UUID;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'event_group' AND column_name = 'event'
  ) THEN
    UPDATE event_group eg
    SET event_uuid = e.uuid
    FROM event e
    WHERE eg.event_uuid IS NULL
      AND eg.event IS NOT NULL
      AND e.id = eg.event;

    IF EXISTS (
      SELECT 1 FROM event_group WHERE event IS NOT NULL AND event_uuid IS NULL
    ) THEN
      RAISE EXCEPTION 'v0016 hard switch: event_group.event_uuid no pudo rellenarse';
    END IF;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'event_group' AND column_name = 'group_salle'
  ) THEN
    UPDATE event_group eg
    SET group_uuid = g.uuid
    FROM group_salle g
    WHERE eg.group_uuid IS NULL
      AND eg.group_salle IS NOT NULL
      AND g.id = eg.group_salle;

    IF EXISTS (
      SELECT 1 FROM event_group WHERE group_salle IS NOT NULL AND group_uuid IS NULL
    ) THEN
      RAISE EXCEPTION 'v0016 hard switch: event_group.group_uuid no pudo rellenarse';
    END IF;
  END IF;
END $$;

DO $$
DECLARE
  rec RECORD;
BEGIN
  FOR rec IN
    SELECT con.conname
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY (con.conkey)
    WHERE rel.relname = 'event_group'
      AND con.contype = 'f'
      AND att.attname IN ('event', 'group_salle')
  LOOP
    EXECUTE format('ALTER TABLE event_group DROP CONSTRAINT IF EXISTS %I', rec.conname);
  END LOOP;
END $$;

ALTER TABLE event_group ALTER COLUMN event_uuid SET NOT NULL;
ALTER TABLE event_group ALTER COLUMN group_uuid SET NOT NULL;
ALTER TABLE event_group DROP COLUMN IF EXISTS event;
ALTER TABLE event_group DROP COLUMN IF EXISTS group_salle;

CREATE INDEX IF NOT EXISTS idx_event_group_event_uuid ON event_group(event_uuid);
CREATE INDEX IF NOT EXISTS idx_event_group_group_uuid ON event_group(group_uuid);

ALTER TABLE event_group DROP CONSTRAINT IF EXISTS uq_event_group;
DROP INDEX IF EXISTS uq_event_group;
CREATE UNIQUE INDEX IF NOT EXISTS uq_event_group_event_group
  ON event_group(event_uuid, group_uuid);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conrelid = 'event_group'::regclass
      AND conname = 'fk_event_group_event'
  ) THEN
    ALTER TABLE event_group
      ADD CONSTRAINT fk_event_group_event
      FOREIGN KEY (event_uuid) REFERENCES event(uuid);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conrelid = 'event_group'::regclass
      AND conname = 'fk_event_group_group'
  ) THEN
    ALTER TABLE event_group
      ADD CONSTRAINT fk_event_group_group
      FOREIGN KEY (group_uuid) REFERENCES group_salle(uuid);
  END IF;
END $$;

-- ---------- event_user(event,user_group_id) -> (event_uuid,user_uuid) ----------
ALTER TABLE event_user ADD COLUMN IF NOT EXISTS event_uuid UUID;
ALTER TABLE event_user ADD COLUMN IF NOT EXISTS user_uuid UUID;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'event_user' AND column_name = 'event'
  ) THEN
    UPDATE event_user eu
    SET event_uuid = e.uuid
    FROM event e
    WHERE eu.event_uuid IS NULL
      AND eu.event IS NOT NULL
      AND e.id = eu.event;

    IF EXISTS (
      SELECT 1 FROM event_user WHERE event IS NOT NULL AND event_uuid IS NULL
    ) THEN
      RAISE EXCEPTION 'v0016 hard switch: event_user.event_uuid no pudo rellenarse';
    END IF;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'event_user' AND column_name = 'user_group_id'
  ) THEN
    UPDATE event_user eu
    SET user_uuid = ug.user_uuid
    FROM user_group ug
    WHERE eu.user_uuid IS NULL
      AND eu.user_group_id IS NOT NULL
      AND ug.id = eu.user_group_id;

    IF EXISTS (
      SELECT 1 FROM event_user WHERE user_group_id IS NOT NULL AND user_uuid IS NULL
    ) THEN
      RAISE EXCEPTION 'v0016 hard switch: event_user.user_uuid no pudo rellenarse';
    END IF;
  END IF;
END $$;

DO $$
DECLARE
  rec RECORD;
BEGIN
  FOR rec IN
    SELECT con.conname
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY (con.conkey)
    WHERE rel.relname = 'event_user'
      AND con.contype = 'f'
      AND att.attname IN ('event', 'user_group_id')
  LOOP
    EXECUTE format('ALTER TABLE event_user DROP CONSTRAINT IF EXISTS %I', rec.conname);
  END LOOP;
END $$;

ALTER TABLE event_user ALTER COLUMN event_uuid SET NOT NULL;
ALTER TABLE event_user ALTER COLUMN user_uuid SET NOT NULL;
ALTER TABLE event_user DROP COLUMN IF EXISTS event;
ALTER TABLE event_user DROP COLUMN IF EXISTS user_group_id;

-- Dedupe (event_uuid, user_uuid): antes del backfill cada fila estaba ligada a
-- un (event, user_group_id). Un mismo usuario en 2 grupos del mismo evento
-- colapsa a la misma pareja (event_uuid, user_uuid). Mantenemos la fila más
-- "verdadera" (status=1 > status!=1, no borrada > borrada, mayor id).
DO $$
DECLARE
  has_id boolean;
BEGIN
  SELECT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'event_user' AND column_name = 'id'
  ) INTO has_id;

  IF has_id THEN
    DELETE FROM event_user
    WHERE id IN (
      SELECT id FROM (
        SELECT id,
               ROW_NUMBER() OVER (
                 PARTITION BY event_uuid, user_uuid
                 ORDER BY (CASE WHEN status = 1 THEN 0 ELSE 1 END) ASC,
                          (CASE WHEN deleted_at IS NULL THEN 0 ELSE 1 END) ASC,
                          id DESC
               ) AS rn
        FROM event_user
      ) ranked
      WHERE ranked.rn > 1
    );
  ELSE
    DELETE FROM event_user eu
    USING event_user dup
    WHERE eu.event_uuid = dup.event_uuid
      AND eu.user_uuid = dup.user_uuid
      AND eu.uuid <> dup.uuid
      AND (
        (CASE WHEN eu.status = 1 THEN 0 ELSE 1 END,
         CASE WHEN eu.deleted_at IS NULL THEN 0 ELSE 1 END,
         eu.uuid)
        >
        (CASE WHEN dup.status = 1 THEN 0 ELSE 1 END,
         CASE WHEN dup.deleted_at IS NULL THEN 0 ELSE 1 END,
         dup.uuid)
      );
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_event_user_event_uuid ON event_user(event_uuid);
CREATE INDEX IF NOT EXISTS idx_event_user_user_uuid ON event_user(user_uuid);

ALTER TABLE event_user DROP CONSTRAINT IF EXISTS uq_event_user;
DROP INDEX IF EXISTS uq_event_user;
CREATE UNIQUE INDEX IF NOT EXISTS uq_event_user_event_user
  ON event_user(event_uuid, user_uuid);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conrelid = 'event_user'::regclass
      AND conname = 'fk_event_user_event'
  ) THEN
    ALTER TABLE event_user
      ADD CONSTRAINT fk_event_user_event
      FOREIGN KEY (event_uuid) REFERENCES event(uuid);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conrelid = 'event_user'::regclass
      AND conname = 'fk_event_user_user'
  ) THEN
    ALTER TABLE event_user
      ADD CONSTRAINT fk_event_user_user
      FOREIGN KEY (user_uuid) REFERENCES user_salle(uuid);
  END IF;
END $$;

-- ---------- weekly_session_user(weekly_session_id,user_group_id) -> (weekly_session_uuid,user_uuid) ----------
ALTER TABLE weekly_session_user ADD COLUMN IF NOT EXISTS weekly_session_uuid UUID;
ALTER TABLE weekly_session_user ADD COLUMN IF NOT EXISTS user_uuid UUID;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'weekly_session_user' AND column_name = 'weekly_session_id'
  ) THEN
    UPDATE weekly_session_user wsu
    SET weekly_session_uuid = ws.uuid
    FROM weekly_session ws
    WHERE wsu.weekly_session_uuid IS NULL
      AND wsu.weekly_session_id IS NOT NULL
      AND ws.id = wsu.weekly_session_id;

    IF EXISTS (
      SELECT 1 FROM weekly_session_user
      WHERE weekly_session_id IS NOT NULL
        AND weekly_session_uuid IS NULL
    ) THEN
      RAISE EXCEPTION 'v0016 hard switch: weekly_session_user.weekly_session_uuid no pudo rellenarse';
    END IF;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'weekly_session_user' AND column_name = 'user_group_id'
  ) THEN
    UPDATE weekly_session_user wsu
    SET user_uuid = ug.user_uuid
    FROM user_group ug
    WHERE wsu.user_uuid IS NULL
      AND wsu.user_group_id IS NOT NULL
      AND ug.id = wsu.user_group_id;

    IF EXISTS (
      SELECT 1 FROM weekly_session_user
      WHERE user_group_id IS NOT NULL
        AND user_uuid IS NULL
    ) THEN
      RAISE EXCEPTION 'v0016 hard switch: weekly_session_user.user_uuid no pudo rellenarse';
    END IF;
  END IF;
END $$;

DO $$
DECLARE
  rec RECORD;
BEGIN
  FOR rec IN
    SELECT con.conname
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY (con.conkey)
    WHERE rel.relname = 'weekly_session_user'
      AND con.contype = 'f'
      AND att.attname IN ('weekly_session_id', 'user_group_id')
  LOOP
    EXECUTE format('ALTER TABLE weekly_session_user DROP CONSTRAINT IF EXISTS %I', rec.conname);
  END LOOP;
END $$;

ALTER TABLE weekly_session_user ALTER COLUMN weekly_session_uuid SET NOT NULL;
ALTER TABLE weekly_session_user ALTER COLUMN user_uuid SET NOT NULL;
ALTER TABLE weekly_session_user DROP COLUMN IF EXISTS weekly_session_id;
ALTER TABLE weekly_session_user DROP COLUMN IF EXISTS user_group_id;

-- Dedupe (weekly_session_uuid, user_uuid): antes del backfill la fila era por
-- user_group. En la práctica cada sesión pertenece a un único grupo, pero si
-- por algún motivo hay duplicados defensivamente colapsamos: preferimos
-- status=1 > !=1, no borrada > borrada, justified=true > false, mayor id.
DO $$
DECLARE
  has_id boolean;
BEGIN
  SELECT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'weekly_session_user' AND column_name = 'id'
  ) INTO has_id;

  IF has_id THEN
    DELETE FROM weekly_session_user
    WHERE id IN (
      SELECT id FROM (
        SELECT id,
               ROW_NUMBER() OVER (
                 PARTITION BY weekly_session_uuid, user_uuid
                 ORDER BY (CASE WHEN status = 1 THEN 0 ELSE 1 END) ASC,
                          (CASE WHEN deleted_at IS NULL THEN 0 ELSE 1 END) ASC,
                          (CASE WHEN justified = true THEN 0 ELSE 1 END) ASC,
                          id DESC
               ) AS rn
        FROM weekly_session_user
      ) ranked
      WHERE ranked.rn > 1
    );
  ELSE
    DELETE FROM weekly_session_user wsu
    USING weekly_session_user dup
    WHERE wsu.weekly_session_uuid = dup.weekly_session_uuid
      AND wsu.user_uuid = dup.user_uuid
      AND wsu.uuid <> dup.uuid
      AND (
        (CASE WHEN wsu.status = 1 THEN 0 ELSE 1 END,
         CASE WHEN wsu.deleted_at IS NULL THEN 0 ELSE 1 END,
         CASE WHEN wsu.justified = true THEN 0 ELSE 1 END,
         wsu.uuid)
        >
        (CASE WHEN dup.status = 1 THEN 0 ELSE 1 END,
         CASE WHEN dup.deleted_at IS NULL THEN 0 ELSE 1 END,
         CASE WHEN dup.justified = true THEN 0 ELSE 1 END,
         dup.uuid)
      );
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_weekly_session_user_weekly_session_uuid ON weekly_session_user(weekly_session_uuid);
CREATE INDEX IF NOT EXISTS idx_weekly_session_user_user_uuid ON weekly_session_user(user_uuid);

ALTER TABLE weekly_session_user DROP CONSTRAINT IF EXISTS uq_weekly_session_user;
DROP INDEX IF EXISTS uq_weekly_session_user;
CREATE UNIQUE INDEX IF NOT EXISTS uq_weekly_session_user_session_user
  ON weekly_session_user(weekly_session_uuid, user_uuid);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conrelid = 'weekly_session_user'::regclass
      AND conname = 'fk_weekly_session_user_weekly_session'
  ) THEN
    ALTER TABLE weekly_session_user
      ADD CONSTRAINT fk_weekly_session_user_weekly_session
      FOREIGN KEY (weekly_session_uuid) REFERENCES weekly_session(uuid);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conrelid = 'weekly_session_user'::regclass
      AND conname = 'fk_weekly_session_user_user'
  ) THEN
    ALTER TABLE weekly_session_user
      ADD CONSTRAINT fk_weekly_session_user_user
      FOREIGN KEY (user_uuid) REFERENCES user_salle(uuid);
  END IF;
END $$;

-- ---------- refresh_token.user_salle -> user_uuid ----------
ALTER TABLE refresh_token ADD COLUMN IF NOT EXISTS user_uuid UUID;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'refresh_token' AND column_name = 'user_salle'
  ) THEN
    UPDATE refresh_token rt
    SET user_uuid = u.uuid
    FROM user_salle u
    WHERE rt.user_uuid IS NULL
      AND rt.user_salle IS NOT NULL
      AND u.id = rt.user_salle;

    IF EXISTS (
      SELECT 1 FROM refresh_token
      WHERE user_salle IS NOT NULL
        AND user_uuid IS NULL
    ) THEN
      RAISE EXCEPTION 'v0016 hard switch: refresh_token.user_uuid no pudo rellenarse';
    END IF;
  END IF;
END $$;

DO $$
DECLARE
  rec RECORD;
BEGIN
  FOR rec IN
    SELECT con.conname
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY (con.conkey)
    WHERE rel.relname = 'refresh_token'
      AND con.contype = 'f'
      AND att.attname = 'user_salle'
  LOOP
    EXECUTE format('ALTER TABLE refresh_token DROP CONSTRAINT IF EXISTS %I', rec.conname);
  END LOOP;
END $$;

ALTER TABLE refresh_token ALTER COLUMN user_uuid SET NOT NULL;
ALTER TABLE refresh_token DROP COLUMN IF EXISTS user_salle;
CREATE INDEX IF NOT EXISTS idx_refresh_token_user_uuid ON refresh_token(user_uuid);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conrelid = 'refresh_token'::regclass
      AND conname = 'fk_refresh_token_user'
  ) THEN
    ALTER TABLE refresh_token
      ADD CONSTRAINT fk_refresh_token_user
      FOREIGN KEY (user_uuid) REFERENCES user_salle(uuid);
  END IF;
END $$;

-- ---------- password_reset_tokens: enlazar opcionalmente con user_uuid ----------
ALTER TABLE password_reset_tokens ADD COLUMN IF NOT EXISTS user_uuid UUID;

UPDATE password_reset_tokens prt
SET user_uuid = u.uuid
FROM user_salle u
WHERE prt.user_uuid IS NULL
  AND lower(prt.email) = lower(u.email);

CREATE INDEX IF NOT EXISTS idx_password_reset_token_user_uuid ON password_reset_tokens(user_uuid);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conrelid = 'password_reset_tokens'::regclass
      AND conname = 'fk_password_reset_tokens_user'
  ) THEN
    ALTER TABLE password_reset_tokens
      ADD CONSTRAINT fk_password_reset_tokens_user
      FOREIGN KEY (user_uuid) REFERENCES user_salle(uuid);
  END IF;
END $$;

-- ---------- user_pending: validar y soltar center_id/group_id ----------
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'user_pending' AND column_name = 'center_id'
  ) AND EXISTS (
    SELECT 1 FROM user_pending WHERE center_id IS NOT NULL AND center_uuid IS NULL
  ) THEN
    RAISE EXCEPTION 'v0016 hard switch: user_pending.center_uuid no pudo rellenarse';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'user_pending' AND column_name = 'group_id'
  ) AND EXISTS (
    SELECT 1 FROM user_pending WHERE group_id IS NOT NULL AND group_uuid IS NULL
  ) THEN
    RAISE EXCEPTION 'v0016 hard switch: user_pending.group_uuid no pudo rellenarse';
  END IF;
END $$;

ALTER TABLE user_pending DROP COLUMN IF EXISTS center_id;
ALTER TABLE user_pending DROP COLUMN IF EXISTS group_id;

--changeset sallejoven:v0016c-parent-pk-switch splitStatements:false
-- ---------- PK switch a uuid ----------
-- Transaccion independiente: requiere que los UPDATE previos que referenciaban
-- los `id` antiguos hayan commiteado sus locks (liberados al terminar el
-- bloque v0016b) antes de poder hacer DROP COLUMN id en cada tabla padre.
DO $$
DECLARE
  pk_name text;
BEGIN
  SELECT conname INTO pk_name
  FROM pg_constraint
  WHERE conrelid = 'center'::regclass AND contype = 'p';
  IF pk_name IS NOT NULL AND pk_name <> 'center_pkey' THEN
    EXECUTE format('ALTER TABLE center DROP CONSTRAINT %I', pk_name);
  ELSIF pk_name = 'center_pkey' AND EXISTS (
    SELECT 1 FROM information_schema.columns WHERE table_name = 'center' AND column_name = 'id'
  ) THEN
    EXECUTE 'ALTER TABLE center DROP CONSTRAINT center_pkey';
  END IF;
END $$;
ALTER TABLE center DROP COLUMN IF EXISTS id;
DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'center'::regclass AND contype = 'p') THEN ALTER TABLE center ADD CONSTRAINT center_pkey PRIMARY KEY (uuid); END IF; END $$;

DO $$
DECLARE
  pk_name text;
BEGIN
  SELECT conname INTO pk_name
  FROM pg_constraint
  WHERE conrelid = 'group_salle'::regclass AND contype = 'p';
  IF pk_name IS NOT NULL AND EXISTS (
    SELECT 1 FROM information_schema.columns WHERE table_name = 'group_salle' AND column_name = 'id'
  ) THEN
    EXECUTE format('ALTER TABLE group_salle DROP CONSTRAINT %I', pk_name);
  END IF;
END $$;
ALTER TABLE group_salle DROP COLUMN IF EXISTS id;
DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'group_salle'::regclass AND contype = 'p') THEN ALTER TABLE group_salle ADD CONSTRAINT group_salle_pkey PRIMARY KEY (uuid); END IF; END $$;

DO $$
DECLARE
  pk_name text;
BEGIN
  SELECT conname INTO pk_name
  FROM pg_constraint
  WHERE conrelid = 'user_salle'::regclass AND contype = 'p';
  IF pk_name IS NOT NULL AND EXISTS (
    SELECT 1 FROM information_schema.columns WHERE table_name = 'user_salle' AND column_name = 'id'
  ) THEN
    EXECUTE format('ALTER TABLE user_salle DROP CONSTRAINT %I', pk_name);
  END IF;
END $$;
ALTER TABLE user_salle DROP COLUMN IF EXISTS id;
DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'user_salle'::regclass AND contype = 'p') THEN ALTER TABLE user_salle ADD CONSTRAINT user_salle_pkey PRIMARY KEY (uuid); END IF; END $$;

DO $$
DECLARE
  pk_name text;
BEGIN
  SELECT conname INTO pk_name
  FROM pg_constraint
  WHERE conrelid = 'vital_situation'::regclass AND contype = 'p';
  IF pk_name IS NOT NULL AND EXISTS (
    SELECT 1 FROM information_schema.columns WHERE table_name = 'vital_situation' AND column_name = 'id'
  ) THEN
    EXECUTE format('ALTER TABLE vital_situation DROP CONSTRAINT %I', pk_name);
  END IF;
END $$;
ALTER TABLE vital_situation DROP COLUMN IF EXISTS id;
DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'vital_situation'::regclass AND contype = 'p') THEN ALTER TABLE vital_situation ADD CONSTRAINT vital_situation_pkey PRIMARY KEY (uuid); END IF; END $$;

DO $$
DECLARE
  pk_name text;
BEGIN
  SELECT conname INTO pk_name
  FROM pg_constraint
  WHERE conrelid = 'vital_situation_session'::regclass AND contype = 'p';
  IF pk_name IS NOT NULL AND EXISTS (
    SELECT 1 FROM information_schema.columns WHERE table_name = 'vital_situation_session' AND column_name = 'id'
  ) THEN
    EXECUTE format('ALTER TABLE vital_situation_session DROP CONSTRAINT %I', pk_name);
  END IF;
END $$;
ALTER TABLE vital_situation_session DROP COLUMN IF EXISTS id;
DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'vital_situation_session'::regclass AND contype = 'p') THEN ALTER TABLE vital_situation_session ADD CONSTRAINT vital_situation_session_pkey PRIMARY KEY (uuid); END IF; END $$;

DO $$
DECLARE
  pk_name text;
BEGIN
  SELECT conname INTO pk_name
  FROM pg_constraint
  WHERE conrelid = 'weekly_session'::regclass AND contype = 'p';
  IF pk_name IS NOT NULL AND EXISTS (
    SELECT 1 FROM information_schema.columns WHERE table_name = 'weekly_session' AND column_name = 'id'
  ) THEN
    EXECUTE format('ALTER TABLE weekly_session DROP CONSTRAINT %I', pk_name);
  END IF;
END $$;
ALTER TABLE weekly_session DROP COLUMN IF EXISTS id;
DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'weekly_session'::regclass AND contype = 'p') THEN ALTER TABLE weekly_session ADD CONSTRAINT weekly_session_pkey PRIMARY KEY (uuid); END IF; END $$;

DO $$
DECLARE
  pk_name text;
BEGIN
  SELECT conname INTO pk_name
  FROM pg_constraint
  WHERE conrelid = 'event'::regclass AND contype = 'p';
  IF pk_name IS NOT NULL AND EXISTS (
    SELECT 1 FROM information_schema.columns WHERE table_name = 'event' AND column_name = 'id'
  ) THEN
    EXECUTE format('ALTER TABLE event DROP CONSTRAINT %I', pk_name);
  END IF;
END $$;
ALTER TABLE event DROP COLUMN IF EXISTS id;
DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'event'::regclass AND contype = 'p') THEN ALTER TABLE event ADD CONSTRAINT event_pkey PRIMARY KEY (uuid); END IF; END $$;

DO $$
DECLARE
  pk_name text;
BEGIN
  SELECT conname INTO pk_name
  FROM pg_constraint
  WHERE conrelid = 'user_group'::regclass AND contype = 'p';
  IF pk_name IS NOT NULL AND EXISTS (
    SELECT 1 FROM information_schema.columns WHERE table_name = 'user_group' AND column_name = 'id'
  ) THEN
    EXECUTE format('ALTER TABLE user_group DROP CONSTRAINT %I', pk_name);
  END IF;
END $$;
ALTER TABLE user_group DROP COLUMN IF EXISTS id;
DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'user_group'::regclass AND contype = 'p') THEN ALTER TABLE user_group ADD CONSTRAINT user_group_pkey PRIMARY KEY (uuid); END IF; END $$;

DO $$
DECLARE
  pk_name text;
BEGIN
  SELECT conname INTO pk_name
  FROM pg_constraint
  WHERE conrelid = 'user_center'::regclass AND contype = 'p';
  IF pk_name IS NOT NULL AND EXISTS (
    SELECT 1 FROM information_schema.columns WHERE table_name = 'user_center' AND column_name = 'id'
  ) THEN
    EXECUTE format('ALTER TABLE user_center DROP CONSTRAINT %I', pk_name);
  END IF;
END $$;
ALTER TABLE user_center DROP COLUMN IF EXISTS id;
DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'user_center'::regclass AND contype = 'p') THEN ALTER TABLE user_center ADD CONSTRAINT user_center_pkey PRIMARY KEY (uuid); END IF; END $$;

DO $$
DECLARE
  pk_name text;
BEGIN
  SELECT conname INTO pk_name
  FROM pg_constraint
  WHERE conrelid = 'event_group'::regclass AND contype = 'p';
  IF pk_name IS NOT NULL AND EXISTS (
    SELECT 1 FROM information_schema.columns WHERE table_name = 'event_group' AND column_name = 'id'
  ) THEN
    EXECUTE format('ALTER TABLE event_group DROP CONSTRAINT %I', pk_name);
  END IF;
END $$;
ALTER TABLE event_group DROP COLUMN IF EXISTS id;
DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'event_group'::regclass AND contype = 'p') THEN ALTER TABLE event_group ADD CONSTRAINT event_group_pkey PRIMARY KEY (uuid); END IF; END $$;

DO $$
DECLARE
  pk_name text;
BEGIN
  SELECT conname INTO pk_name
  FROM pg_constraint
  WHERE conrelid = 'event_user'::regclass AND contype = 'p';
  IF pk_name IS NOT NULL AND EXISTS (
    SELECT 1 FROM information_schema.columns WHERE table_name = 'event_user' AND column_name = 'id'
  ) THEN
    EXECUTE format('ALTER TABLE event_user DROP CONSTRAINT %I', pk_name);
  END IF;
END $$;
ALTER TABLE event_user DROP COLUMN IF EXISTS id;
DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'event_user'::regclass AND contype = 'p') THEN ALTER TABLE event_user ADD CONSTRAINT event_user_pkey PRIMARY KEY (uuid); END IF; END $$;

DO $$
DECLARE
  pk_name text;
BEGIN
  SELECT conname INTO pk_name
  FROM pg_constraint
  WHERE conrelid = 'weekly_session_user'::regclass AND contype = 'p';
  IF pk_name IS NOT NULL AND EXISTS (
    SELECT 1 FROM information_schema.columns WHERE table_name = 'weekly_session_user' AND column_name = 'id'
  ) THEN
    EXECUTE format('ALTER TABLE weekly_session_user DROP CONSTRAINT %I', pk_name);
  END IF;
END $$;
ALTER TABLE weekly_session_user DROP COLUMN IF EXISTS id;
DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'weekly_session_user'::regclass AND contype = 'p') THEN ALTER TABLE weekly_session_user ADD CONSTRAINT weekly_session_user_pkey PRIMARY KEY (uuid); END IF; END $$;

DO $$
DECLARE
  pk_name text;
BEGIN
  SELECT conname INTO pk_name
  FROM pg_constraint
  WHERE conrelid = 'refresh_token'::regclass AND contype = 'p';
  IF pk_name IS NOT NULL AND EXISTS (
    SELECT 1 FROM information_schema.columns WHERE table_name = 'refresh_token' AND column_name = 'id'
  ) THEN
    EXECUTE format('ALTER TABLE refresh_token DROP CONSTRAINT %I', pk_name);
  END IF;
END $$;
ALTER TABLE refresh_token DROP COLUMN IF EXISTS id;
DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'refresh_token'::regclass AND contype = 'p') THEN ALTER TABLE refresh_token ADD CONSTRAINT refresh_token_pkey PRIMARY KEY (uuid); END IF; END $$;

DO $$
DECLARE
  pk_name text;
BEGIN
  SELECT conname INTO pk_name
  FROM pg_constraint
  WHERE conrelid = 'password_reset_tokens'::regclass AND contype = 'p';
  IF pk_name IS NOT NULL AND EXISTS (
    SELECT 1 FROM information_schema.columns WHERE table_name = 'password_reset_tokens' AND column_name = 'id'
  ) THEN
    EXECUTE format('ALTER TABLE password_reset_tokens DROP CONSTRAINT %I', pk_name);
  END IF;
END $$;
ALTER TABLE password_reset_tokens DROP COLUMN IF EXISTS id;
DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'password_reset_tokens'::regclass AND contype = 'p') THEN ALTER TABLE password_reset_tokens ADD CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (uuid); END IF; END $$;

DO $$
DECLARE
  pk_name text;
BEGIN
  SELECT conname INTO pk_name
  FROM pg_constraint
  WHERE conrelid = 'historical_data'::regclass AND contype = 'p';
  IF pk_name IS NOT NULL AND EXISTS (
    SELECT 1 FROM information_schema.columns WHERE table_name = 'historical_data' AND column_name = 'id'
  ) THEN
    EXECUTE format('ALTER TABLE historical_data DROP CONSTRAINT %I', pk_name);
  END IF;
END $$;
ALTER TABLE historical_data DROP COLUMN IF EXISTS id;
DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'historical_data'::regclass AND contype = 'p') THEN ALTER TABLE historical_data ADD CONSTRAINT historical_data_pkey PRIMARY KEY (uuid); END IF; END $$;

DO $$
DECLARE
  pk_name text;
BEGIN
  SELECT conname INTO pk_name
  FROM pg_constraint
  WHERE conrelid = 'user_pending'::regclass AND contype = 'p';
  IF pk_name IS NOT NULL AND EXISTS (
    SELECT 1 FROM information_schema.columns WHERE table_name = 'user_pending' AND column_name = 'id'
  ) THEN
    EXECUTE format('ALTER TABLE user_pending DROP CONSTRAINT %I', pk_name);
  END IF;
END $$;
ALTER TABLE user_pending DROP COLUMN IF EXISTS id;
DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'user_pending'::regclass AND contype = 'p') THEN ALTER TABLE user_pending ADD CONSTRAINT user_pending_pkey PRIMARY KEY (uuid); END IF; END $$;

DROP FUNCTION IF EXISTS sallejoven_backfill_uuid();
