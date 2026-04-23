ALTER TABLE weekly_session_behavior_warning
    ADD COLUMN IF NOT EXISTS created_by_name VARCHAR(200);

UPDATE weekly_session_behavior_warning warning
SET created_by_name = LEFT(
    TRIM(
        CONCAT_WS(
            ' ',
            NULLIF(BTRIM(created_by.name), ''),
            NULLIF(BTRIM(created_by.last_name), '')
        )
    ),
    200
)
FROM user_salle created_by
WHERE created_by.uuid = warning.created_by_user_uuid
  AND (warning.created_by_name IS NULL OR BTRIM(warning.created_by_name) = '');

UPDATE weekly_session_behavior_warning
SET created_by_name = 'Usuario desconocido'
WHERE created_by_name IS NULL OR BTRIM(created_by_name) = '';

ALTER TABLE weekly_session_behavior_warning
    ALTER COLUMN created_by_name SET NOT NULL;
