CREATE TABLE IF NOT EXISTS system_settings (
    id SERIAL PRIMARY KEY,
    deletion_threshold NUMERIC(3, 2) NOT NULL DEFAULT 1.0
);

INSERT INTO system_settings (id, deletion_threshold) VALUES (1, 1.0) ON CONFLICT DO NOTHING;
