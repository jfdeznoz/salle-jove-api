
ALTER TABLE user_salle
  ADD COLUMN IF NOT EXISTS is_admin BOOLEAN NOT NULL DEFAULT FALSE;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name='user_salle' AND column_name='roles'
  ) THEN
    UPDATE user_salle SET is_admin = TRUE
    WHERE UPPER(roles) LIKE '%ROLE_ADMIN%';
  END IF;
END $$;
