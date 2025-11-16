-- Базовая схема музыкальной платформы (без дропа существующих объектов)
CREATE TYPE roletype AS ENUM ('user','admin');

CREATE TABLE users(
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    registration_date TIMESTAMP NOT NULL DEFAULT now(),
    password_hash BYTEA NOT NULL,
    password_salt BYTEA NOT NULL,
    password_alg VARCHAR(20) NOT NULL DEFAULT 'SHA-384',
    avatar_path VARCHAR(255),
    role SMALLINT NOT NULL DEFAULT 0
);

CREATE TABLE label(
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    description TEXT,
    avg_rating REAL NOT NULL DEFAULT 0
);

CREATE TABLE genre(
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    color CHAR(7),
    avg_rating REAL NOT NULL DEFAULT 0
);

CREATE TABLE album(
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    upload_date TIMESTAMP NOT NULL DEFAULT now(),
    cover_url VARCHAR(255),
    genre BIGINT REFERENCES genre(id) NOT NULL,
    track_number INTEGER NOT NULL,
    explicit BOOLEAN NOT NULL DEFAULT FALSE,
    inoagent BOOLEAN NOT NULL DEFAULT FALSE,
    avg_rating REAL NOT NULL DEFAULT 0,
    label_id BIGINT REFERENCES label(id) NOT NULL,
    publication_date TIMESTAMP NOT NULL
);

CREATE TABLE artist(
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    creation_date TIMESTAMP NOT NULL DEFAULT now(),
    avatar_path VARCHAR(255)
);

CREATE TABLE track(
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    author_id BIGINT REFERENCES users(id) NOT NULL,
    duration INTEGER NOT NULL,
    filesize BIGINT NOT NULL,
    rating BIGINT NOT NULL DEFAULT 0,
    rate_count INTEGER NOT NULL DEFAULT 0,
    upload_date TIMESTAMP NOT NULL DEFAULT now(),
    album_id BIGINT REFERENCES album(id) NOT NULL,
    artist_id BIGINT REFERENCES artist(id) NOT NULL,
    genre_id BIGINT REFERENCES genre(id) NOT NULL,
    filepath VARCHAR(255) NOT NULL,
    number_on_album INTEGER NOT NULL,
    explicit BOOLEAN NOT NULL DEFAULT FALSE,
    inoagent BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE role(
    id SMALLSERIAL PRIMARY KEY,
    role roletype NOT NULL,
    description TEXT NOT NULL
);

CREATE TABLE liked(
    user_id BIGINT references users(id) NOT NULL,
    track_id BIGINT references track(id) NOT NULL,
    PRIMARY KEY (user_id, track_id),
    created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE user_rating(
    user_id BIGINT references users(id) NOT NULL,
    track_id BIGINT references track(id) NOT NULL,
    PRIMARY KEY (user_id, track_id),
    rate SMALLINT NOT NULL CHECK(rate BETWEEN 1 AND 5)
);

CREATE TABLE comments(
    user_id BIGINT references users(id) NOT NULL,
    track_id BIGINT references track(id) NOT NULL,
    PRIMARY KEY (user_id, track_id),
    body TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    track_time INT NOT NULL
);

CREATE TABLE artist_track(
    artist_id BIGINT REFERENCES artist(id) NOT NULL,
    track_id BIGINT REFERENCES track(id) NOT NULL,
    PRIMARY KEY (artist_id, track_id)
);

CREATE TABLE track_genre(
    track_id BIGINT REFERENCES track(id) NOT NULL,
    genre_id BIGINT REFERENCES genre(id) NOT NULL,
    PRIMARY KEY (track_id, genre_id)
);
