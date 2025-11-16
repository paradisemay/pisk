-- Ensure actor references use foreign keys to users
ALTER TABLE audit_log
    ADD COLUMN IF NOT EXISTS actor_id BIGINT,
    DROP CONSTRAINT IF EXISTS fk_audit_actor,
    DROP CONSTRAINT IF EXISTS audit_log_actor_id_fkey;

ALTER TABLE track_status_history
    ADD COLUMN IF NOT EXISTS actor_id BIGINT,
    DROP CONSTRAINT IF EXISTS fk_track_status_history_actor,
    DROP CONSTRAINT IF EXISTS track_status_history_actor_id_fkey;

ALTER TABLE audit_log
    ALTER COLUMN actor_id SET NOT NULL,
    ADD CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users(id);

ALTER TABLE track_status_history
    ALTER COLUMN actor_id SET NOT NULL,
    ADD CONSTRAINT fk_track_status_history_actor FOREIGN KEY (actor_id) REFERENCES users(id);
