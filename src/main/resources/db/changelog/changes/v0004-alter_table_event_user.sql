--------------------------------------------
-- 0) DEFENSIVO: soltar FK dependiente si existe (para no bloquear user_group)
--------------------------------------------
ALTER TABLE event_user DROP CONSTRAINT IF EXISTS fk_event_user_user_group;

--------------------------------------------
-- 1) USER_GROUP
--------------------------------------------
-- Columnas nuevas
ALTER TABLE user_group
  ADD COLUMN IF NOT EXISTS id SERIAL;

ALTER TABLE user_group
  ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- Rellenar id si está NULL (para datos existentes)
UPDATE user_group
SET id = COALESCE(id, nextval(pg_get_serial_sequence('user_group','id')))
WHERE id IS NULL;

-- (Re)crear PK sobre id (ahora ya no hay FKs dependientes)
ALTER TABLE user_group DROP CONSTRAINT IF EXISTS user_group_pkey;
ALTER TABLE user_group ADD CONSTRAINT user_group_pkey PRIMARY KEY (id);

-- FKs: primero limpia, luego crea (idempotente)
ALTER TABLE user_group DROP CONSTRAINT IF EXISTS fk_user_group_user;
ALTER TABLE user_group DROP CONSTRAINT IF EXISTS fk_user_group_group;

ALTER TABLE user_group
  ADD CONSTRAINT fk_user_group_user
    FOREIGN KEY (user_salle) REFERENCES user_salle(id),
  ADD CONSTRAINT fk_user_group_group
    FOREIGN KEY (group_salle) REFERENCES group_salle(id);

-- Índices informativos
CREATE INDEX IF NOT EXISTS idx_user_group_user   ON user_group(user_salle);
CREATE INDEX IF NOT EXISTS idx_user_group_group  ON user_group(group_salle);

--------------------------------------------
-- 2) EVENT_GROUP
--------------------------------------------
-- Columnas y PK
ALTER TABLE event_group
  ADD COLUMN IF NOT EXISTS id SERIAL;

UPDATE event_group
SET id = COALESCE(id, nextval(pg_get_serial_sequence('event_group','id')))
WHERE id IS NULL;

ALTER TABLE event_group DROP CONSTRAINT IF EXISTS event_group_pkey;
ALTER TABLE event_group ADD CONSTRAINT event_group_pkey PRIMARY KEY (id);

-- Limpieza defensiva por si quedó un índice o constraint con el nombre
ALTER TABLE event_group DROP CONSTRAINT IF EXISTS uq_event_group;
DROP INDEX IF EXISTS uq_event_group;

-- Crear UNIQUE(event, group_salle) SOLO si no existe ya (por nombre exacto)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint c
    WHERE c.conrelid = 'event_group'::regclass
      AND c.contype = 'u'
      AND c.conname = 'uq_event_group'
  ) THEN
    -- Aun así, puede existir un índice UNIQUE equivalente con otro nombre.
    -- Si queremos asegurarnos por columnas, comprobamos a nivel de índice:
    IF NOT EXISTS (
      SELECT 1
      FROM pg_index i
      JOIN pg_class t ON t.oid = i.indrelid
      WHERE t.relname = 'event_group'
        AND i.indisunique
        AND i.indkey::text = (
          SELECT array_to_string(ARRAY[
            (SELECT attnum FROM pg_attribute WHERE attrelid = t.oid AND attname = 'event'),
            (SELECT attnum FROM pg_attribute WHERE attrelid = t.oid AND attname = 'group_salle')
          ], ' ')
        )
    ) THEN
      ALTER TABLE event_group
        ADD CONSTRAINT uq_event_group UNIQUE (event, group_salle);
    END IF;
  END IF;
END$$;

-- Índices
CREATE INDEX IF NOT EXISTS idx_event_group_event ON event_group(event);
CREATE INDEX IF NOT EXISTS idx_event_group_group ON event_group(group_salle);

--------------------------------------------
-- 3) EVENT_USER
--------------------------------------------
-- Columnas nuevas
ALTER TABLE event_user
  ADD COLUMN IF NOT EXISTS id SERIAL;

ALTER TABLE event_user
  ADD COLUMN IF NOT EXISTS user_group_id BIGINT;

-- FK a user_group(id) idempotente (por nombre)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint c
    WHERE c.conrelid = 'event_user'::regclass
      AND c.conname = 'fk_event_user_user_group'
  ) THEN
    ALTER TABLE event_user
      ADD CONSTRAINT fk_event_user_user_group
        FOREIGN KEY (user_group_id) REFERENCES user_group(id);
  END IF;
END$$;

-- UNIQUE(event, user_group_id) idempotente
-- Limpiamos nombre por si quedó sólo el índice
ALTER TABLE event_user DROP CONSTRAINT IF EXISTS uq_event_user;
DROP INDEX IF EXISTS uq_event_user;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_index i
    JOIN pg_class t ON t.oid = i.indrelid
    WHERE t.relname = 'event_user'
      AND i.indisunique
      AND i.indkey::text = (
        SELECT array_to_string(ARRAY[
          (SELECT attnum FROM pg_attribute WHERE attrelid = t.oid AND attname = 'event'),
          (SELECT attnum FROM pg_attribute WHERE attrelid = t.oid AND attname = 'user_group_id')
        ], ' ')
      )
  ) THEN
    ALTER TABLE event_user
      ADD CONSTRAINT uq_event_user UNIQUE (event, user_group_id);
  END IF;
END$$;

-- Reemplazar PK por id
ALTER TABLE event_user DROP CONSTRAINT IF EXISTS event_user_pkey;
ALTER TABLE event_user ADD CONSTRAINT event_user_pkey PRIMARY KEY (id);

-- Índices
CREATE INDEX IF NOT EXISTS idx_event_user_event       ON event_user(event);
CREATE INDEX IF NOT EXISTS idx_event_user_user_group  ON event_user(user_group_id);

-- Eliminar viejas FKs si existen (por si quedaron de intentos anteriores)
ALTER TABLE event_user DROP CONSTRAINT IF EXISTS fk_event_user_user_salle;
ALTER TABLE event_user DROP CONSTRAINT IF EXISTS fk_event_user_group_salle;
ALTER TABLE event_user DROP CONSTRAINT IF EXISTS fk_event_user_user_group_comp;

-- Eliminar columnas antiguas (ya no se usan)
ALTER TABLE event_user DROP COLUMN IF EXISTS user_salle;
ALTER TABLE event_user DROP COLUMN IF EXISTS group_salle;
