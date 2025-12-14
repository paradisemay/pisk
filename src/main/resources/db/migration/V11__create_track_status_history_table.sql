CREATE TABLE IF NOT EXISTS track_status_history(
    id BIGSERIAL PRIMARY KEY,
    track_id BIGINT NOT NULL REFERENCES track(id),
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT now(),
    actor_id BIGINT NOT NULL REFERENCES users(id)
);
