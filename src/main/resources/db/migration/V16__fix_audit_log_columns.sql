ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS artist_id BIGINT REFERENCES artist(id);
ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS user_id BIGINT REFERENCES users(id);

DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'audit_log' AND column_name = 'actor_id') THEN
        UPDATE audit_log SET user_id = actor_id WHERE actor_id IS NOT NULL;
    END IF;
END $$;

ALTER TABLE audit_log DROP COLUMN IF EXISTS actor_id;
