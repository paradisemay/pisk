CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    email VARCHAR(128) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    last_login_at TIMESTAMP
);

CREATE TABLE user_subscriptions (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    artist_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, artist_id)
);

CREATE TABLE genre (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(512)
);

CREATE TABLE album (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(128) NOT NULL,
    description VARCHAR(1024),
    release_date DATE,
    cover_url VARCHAR(255),
    owner_id BIGINT REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE track (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(128) NOT NULL,
    duration_seconds BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    album_id BIGINT REFERENCES album(id) ON DELETE SET NULL,
    artist_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    storage_path VARCHAR(255) NOT NULL,
    content_type VARCHAR(64) NOT NULL,
    file_size BIGINT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    average_rating DOUBLE PRECISION DEFAULT 0,
    ratings_count BIGINT DEFAULT 0,
    play_count BIGINT DEFAULT 0
);

CREATE TABLE album_genre (
    album_id BIGINT NOT NULL REFERENCES album(id) ON DELETE CASCADE,
    genre_id BIGINT NOT NULL REFERENCES genre(id) ON DELETE CASCADE,
    PRIMARY KEY (album_id, genre_id)
);

CREATE TABLE track_genre (
    track_id BIGINT NOT NULL REFERENCES track(id) ON DELETE CASCADE,
    genre_id BIGINT NOT NULL REFERENCES genre(id) ON DELETE CASCADE,
    PRIMARY KEY (track_id, genre_id)
);

CREATE TABLE subscriptions (
    id BIGSERIAL PRIMARY KEY,
    follower_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    artist_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (follower_id, artist_id)
);

CREATE TABLE user_rating (
    id BIGSERIAL PRIMARY KEY,
    track_id BIGINT NOT NULL REFERENCES track(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    value INTEGER NOT NULL,
    comment VARCHAR(1024),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_user_rating_value CHECK (value BETWEEN 1 AND 5),
    CONSTRAINT uk_user_rating_track_user UNIQUE (track_id, user_id)
);

CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    track_id BIGINT NOT NULL REFERENCES track(id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    body VARCHAR(2048) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    complaint_count INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE liked (
    id BIGSERIAL PRIMARY KEY,
    track_id BIGINT NOT NULL REFERENCES track(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_liked_track_user UNIQUE (track_id, user_id)
);

CREATE TABLE complaints (
    id BIGSERIAL PRIMARY KEY,
    track_id BIGINT NOT NULL REFERENCES track(id) ON DELETE CASCADE,
    complainant_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason VARCHAR(2048) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    moderator_note VARCHAR(2048)
);

CREATE TABLE track_status_history (
    id BIGSERIAL PRIMARY KEY,
    track_id BIGINT NOT NULL REFERENCES track(id) ON DELETE CASCADE,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor_id BIGINT
);

CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(32) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    actor_id BIGINT,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_metadata (
    audit_id BIGINT NOT NULL REFERENCES audit_log(id) ON DELETE CASCADE,
    meta_key VARCHAR(255) NOT NULL,
    meta_value VARCHAR(255),
    PRIMARY KEY (audit_id, meta_key)
);

CREATE INDEX idx_track_title_trgm ON track USING gin (title gin_trgm_ops);
CREATE INDEX idx_album_title_trgm ON album USING gin (title gin_trgm_ops);
CREATE INDEX idx_comments_body_trgm ON comments USING gin (body gin_trgm_ops);
