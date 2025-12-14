-- Add play_count and status columns to track table
ALTER TABLE track
    ADD COLUMN IF NOT EXISTS play_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';

