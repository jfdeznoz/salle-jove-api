DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name='user_salle' AND column_name='roles'
  ) THEN
    ALTER TABLE user_salle DROP COLUMN roles;
  END IF;
END $$;