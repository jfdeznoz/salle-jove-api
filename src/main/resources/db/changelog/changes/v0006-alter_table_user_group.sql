ALTER TABLE user_group
  ADD COLUMN IF NOT EXISTS year INTEGER;

UPDATE user_group
SET year = EXTRACT(YEAR FROM CURRENT_DATE)::INT - 1
WHERE year IS NULL;

ALTER TABLE user_group
  ALTER COLUMN year SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_user_group_year ON user_group(year);



CREATE TABLE IF NOT EXISTS academic_state (
  id          SMALLINT PRIMARY KEY DEFAULT 1,  -- siempre 1
  visible_year INT NOT NULL,
  promoted_at  TIMESTAMP NULL
);

INSERT INTO academic_state (id, visible_year, promoted_at)
VALUES (1, EXTRACT(YEAR FROM CURRENT_DATE)::INT - 1, NULL)
ON CONFLICT (id) DO NOTHING;