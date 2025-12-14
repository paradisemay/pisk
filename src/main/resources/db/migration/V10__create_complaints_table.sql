CREATE TABLE IF NOT EXISTS complaints(
    id BIGSERIAL PRIMARY KEY,
    track_id BIGINT NOT NULL REFERENCES track(id),
    complainant_id BIGINT NOT NULL REFERENCES users(id),
    reason VARCHAR(2048) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    resolved_at TIMESTAMP,
    moderator_note VARCHAR(2048)
);
