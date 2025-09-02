ALTER TABLE user_group ADD COLUMN IF NOT EXISTS user_type SMALLINT;

UPDATE user_group SET user_type = 0 WHERE user_type IS NULL;

WITH flags AS (
  SELECT u.id AS user_id,
         COALESCE(u.roles,'') ~* '(^|[ ,])(?:ROLE_)?PASTORAL_DELEGATE([ ,]|$)' AS has_pd
  FROM user_salle u
)
UPDATE user_group ug
SET user_type = 3
FROM flags f
WHERE f.user_id = ug.user_salle
  AND f.has_pd = TRUE;

WITH flags AS (
  SELECT u.id AS user_id,
         COALESCE(u.roles,'') ~* '(^|[ ,])(?:ROLE_)?GROUP_LEADER([ ,]|$)' AS has_gl
  FROM user_salle u
)
UPDATE user_group ug
SET user_type = 2
FROM flags f
WHERE f.user_id = ug.user_salle
  AND f.has_gl = TRUE
  AND ug.user_type <> 3;

WITH flags AS (
  SELECT u.id AS user_id,
         COALESCE(u.roles,'') ~* '(^|[ ,])(?:ROLE_)?ANIMATOR([ ,]|$)' AS has_anim
  FROM user_salle u
)
UPDATE user_group ug
SET user_type = 1
FROM flags f
WHERE f.user_id = ug.user_salle
  AND f.has_anim = TRUE
  AND ug.user_type NOT IN (2,3);

-- 3) NOT NULL, DEFAULT y CHECK
ALTER TABLE user_group ALTER COLUMN user_type SET NOT NULL;
ALTER TABLE user_group ALTER COLUMN user_type SET DEFAULT 0;

