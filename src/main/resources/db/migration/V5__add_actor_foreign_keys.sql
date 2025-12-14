-- Ensure actor references use foreign keys to users
-- Only modify tables if they exist (for development flexibility)

DO $$
BEGIN
    -- Handle audit_log table if it exists
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'audit_log') THEN
        ALTER TABLE audit_log
            ADD COLUMN IF NOT EXISTS actor_id BIGINT,
            DROP CONSTRAINT IF EXISTS fk_audit_actor,
            DROP CONSTRAINT IF EXISTS audit_log_actor_id_fkey;
        
        -- Only set NOT NULL and add constraint if column exists
        IF EXISTS (SELECT FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = 'audit_log' AND column_name = 'actor_id') THEN
            ALTER TABLE audit_log
                ALTER COLUMN actor_id DROP NOT NULL;
            ALTER TABLE audit_log
                ALTER COLUMN actor_id SET NOT NULL;
            ALTER TABLE audit_log
                ADD CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users(id);
        END IF;
    END IF;

    -- Handle track_status_history table if it exists
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'track_status_history') THEN
        ALTER TABLE track_status_history
            ADD COLUMN IF NOT EXISTS actor_id BIGINT,
            DROP CONSTRAINT IF EXISTS fk_track_status_history_actor,
            DROP CONSTRAINT IF EXISTS track_status_history_actor_id_fkey;
        
        -- Only set NOT NULL and add constraint if column exists
        IF EXISTS (SELECT FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = 'track_status_history' AND column_name = 'actor_id') THEN
            ALTER TABLE track_status_history
                ALTER COLUMN actor_id DROP NOT NULL;
            ALTER TABLE track_status_history
                ALTER COLUMN actor_id SET NOT NULL;
            ALTER TABLE track_status_history
                ADD CONSTRAINT fk_track_status_history_actor FOREIGN KEY (actor_id) REFERENCES users(id);
        END IF;
    END IF;
END $$;
