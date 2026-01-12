CREATE TABLE IF NOT EXISTS system_settings (
    id SERIAL PRIMARY KEY,
    deletion_threshold NUMERIC(3, 2) NOT NULL DEFAULT 3.00
);

INSERT INTO system_settings (id, deletion_threshold) VALUES (1, 3.00)
ON CONFLICT (id) DO NOTHING;
