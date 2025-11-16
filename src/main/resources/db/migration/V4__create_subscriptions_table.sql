DROP TABLE IF EXISTS user_subscriptions;

CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGSERIAL PRIMARY KEY,
    follower_id BIGINT NOT NULL REFERENCES users(id),
    artist_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_subscriptions_follower_artist UNIQUE (follower_id, artist_id)
);
