ALTER TABLE weekly_session_user
  ALTER COLUMN status DROP NOT NULL;

UPDATE weekly_session_user wsu
SET status = NULL,
    justified = FALSE,
    justification_reason = NULL
FROM weekly_session ws
WHERE ws.uuid = wsu.weekly_session_uuid
  AND wsu.status = 0
  AND COALESCE(wsu.justified, FALSE) = FALSE
  AND ws.session_datetime >= (timezone('Europe/Madrid', now())::date)::timestamp;
