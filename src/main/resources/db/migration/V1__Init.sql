-- ===================================================================
-- Consolidated Database Schema for Music Platform (V1 Init)
-- Replaces previous migrations V1-V16
-- ===================================================================

-- Clean start
DROP TABLE IF EXISTS audit_metadata CASCADE;
DROP TABLE IF EXISTS audit_log CASCADE;
DROP TABLE IF EXISTS track_status_history CASCADE;
DROP TABLE IF EXISTS complaints CASCADE;
DROP TABLE IF EXISTS subscriptions CASCADE;
DROP TABLE IF EXISTS track_genre CASCADE;
DROP TABLE IF EXISTS artist_track CASCADE;
DROP TABLE IF EXISTS comments CASCADE;
DROP TABLE IF EXISTS user_rating CASCADE;
DROP TABLE IF EXISTS liked CASCADE;
DROP TABLE IF EXISTS track CASCADE;
DROP TABLE IF EXISTS album CASCADE;
DROP TABLE IF EXISTS artist CASCADE;
DROP TABLE IF EXISTS genre CASCADE;
DROP TABLE IF EXISTS label CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS role CASCADE; -- Old V1 table, dropped
DROP TYPE IF EXISTS roletype; -- Old V1 type, dropped

-- 1. Users
CREATE TABLE users(
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) UNIQUE NOT NULL,
    email VARCHAR(128) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    password_salt VARCHAR(64) NOT NULL,
    password_alg VARCHAR(20) DEFAULT 'SHA-384' NOT NULL,
    avatar_path VARCHAR(255),
    role VARCHAR(16) DEFAULT 'USER' NOT NULL,
    status VARCHAR(16) DEFAULT 'PENDING' NOT NULL,
    created_at TIMESTAMP DEFAULT now() NOT NULL,
    updated_at TIMESTAMP DEFAULT now() NOT NULL,
    last_login_at TIMESTAMP
);

-- 2. Label
CREATE TABLE label(
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) UNIQUE NOT NULL,
    description TEXT,
    avg_rating REAL DEFAULT 0 NOT NULL
);

-- 3. Genre
CREATE TABLE genre(
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) UNIQUE NOT NULL,
    description TEXT,
    color CHAR(7),
    avg_rating REAL DEFAULT 0 NOT NULL
);

-- 4. Artist
CREATE TABLE artist(
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    creation_date TIMESTAMP DEFAULT now() NOT NULL,
    avatar_path VARCHAR(255),
    label_id BIGINT REFERENCES label(id)
);

-- 5. Album
CREATE TABLE album(
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    upload_date TIMESTAMP DEFAULT now() NOT NULL,
    cover_url VARCHAR(255),
    genre BIGINT REFERENCES genre(id) NOT NULL,
    track_number INTEGER NOT NULL,
    explicit BOOLEAN DEFAULT FALSE NOT NULL,
    inoagent BOOLEAN DEFAULT FALSE NOT NULL,
    avg_rating REAL DEFAULT 0 NOT NULL,
    label_id BIGINT REFERENCES label(id) NOT NULL,
    publication_date TIMESTAMP NOT NULL
);

-- 6. Track
CREATE TABLE track(
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    author_id BIGINT REFERENCES users(id) NOT NULL,
    duration INTEGER NOT NULL,
    filesize BIGINT NOT NULL,
    rating BIGINT DEFAULT 0 NOT NULL,
    rate_count INTEGER DEFAULT 0 NOT NULL,
    upload_date TIMESTAMP DEFAULT now() NOT NULL,
    album_id BIGINT REFERENCES album(id) NOT NULL,
    artist_id BIGINT REFERENCES artist(id) NOT NULL,
    genre_id BIGINT REFERENCES genre(id) NOT NULL,
    filepath VARCHAR(255) NOT NULL,
    number_on_album INTEGER NOT NULL,
    explicit BOOLEAN DEFAULT FALSE NOT NULL,
    inoagent BOOLEAN DEFAULT FALSE NOT NULL,
    content_type VARCHAR(255),
    original_filename VARCHAR(255),
    play_count BIGINT DEFAULT 0 NOT NULL,
    status VARCHAR(20) DEFAULT 'DRAFT' NOT NULL
);

-- 7. Liked
CREATE TABLE liked(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) NOT NULL,
    track_id BIGINT REFERENCES track(id) NOT NULL,
    created_at TIMESTAMP DEFAULT now() NOT NULL,
    CONSTRAINT uk_liked_track_user UNIQUE (track_id, user_id)
);

-- 8. User Rating
CREATE TABLE user_rating(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) NOT NULL,
    track_id BIGINT REFERENCES track(id) NOT NULL,
    value SMALLINT NOT NULL CHECK(value BETWEEN 1 AND 5),
    comment VARCHAR(1024),
    created_at TIMESTAMP DEFAULT now() NOT NULL,
    CONSTRAINT uk_user_rating_track_user UNIQUE (track_id, user_id)
);

-- 9. Comments
CREATE TABLE comments(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) NOT NULL,
    track_id BIGINT REFERENCES track(id) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT now() NOT NULL,
    track_time INTEGER,
    updated_at TIMESTAMP,
    complaint_count INTEGER DEFAULT 0 NOT NULL,
    CONSTRAINT uk_comments_user_track UNIQUE (user_id, track_id)
);

-- 10. Artist Track (Many-to-Many)
CREATE TABLE artist_track(
    artist_id BIGINT REFERENCES artist(id) NOT NULL,
    track_id BIGINT REFERENCES track(id) NOT NULL,
    PRIMARY KEY (artist_id, track_id)
);

-- 11. Track Genre (Many-to-Many)
CREATE TABLE track_genre(
    track_id BIGINT REFERENCES track(id) NOT NULL,
    genre_id BIGINT REFERENCES genre(id) NOT NULL,
    PRIMARY KEY (track_id, genre_id)
);

-- 12. Subscriptions
CREATE TABLE subscriptions(
    id BIGSERIAL PRIMARY KEY,
    follower_id BIGINT NOT NULL REFERENCES users(id),
    artist_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(16) DEFAULT 'ACTIVE' NOT NULL,
    created_at TIMESTAMP DEFAULT now() NOT NULL,
    CONSTRAINT uk_subscriptions_follower_artist UNIQUE (follower_id, artist_id)
);

-- 13. Complaints
CREATE TABLE complaints(
    id BIGSERIAL PRIMARY KEY,
    track_id BIGINT NOT NULL REFERENCES track(id),
    complainant_id BIGINT NOT NULL REFERENCES users(id),
    reason VARCHAR(2048) NOT NULL,
    status VARCHAR(16) DEFAULT 'OPEN' NOT NULL,
    created_at TIMESTAMP DEFAULT now() NOT NULL,
    resolved_at TIMESTAMP,
    moderator_note VARCHAR(2048)
);

-- 14. Track Status History
CREATE TABLE track_status_history(
    id BIGSERIAL PRIMARY KEY,
    track_id BIGINT NOT NULL REFERENCES track(id),
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    changed_at TIMESTAMP DEFAULT now() NOT NULL,
    actor_id BIGINT NOT NULL REFERENCES users(id)
);

-- 15. Audit Log
CREATE TABLE audit_log(
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(32) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMP DEFAULT now() NOT NULL,
    user_id BIGINT REFERENCES users(id),
    artist_id BIGINT REFERENCES artist(id)
);

-- 16. Audit Metadata
CREATE TABLE audit_metadata(
    audit_id BIGINT NOT NULL REFERENCES audit_log(id) ON DELETE CASCADE,
    meta_key VARCHAR(255) NOT NULL,
    meta_value VARCHAR(255),
    PRIMARY KEY (audit_id, meta_key)
);


-- ===================================================================
-- STORED FUNCTIONS & PROCEDURES
-- ===================================================================

-- Utils

CREATE OR REPLACE FUNCTION fn_is_admin(p_user_id bigint)
RETURNS boolean
LANGUAGE sql
AS $$
  SELECT EXISTS (
    SELECT 1 FROM users u
    WHERE u.id = p_user_id AND u.role = 'ADMIN'
  );
$$;

CREATE OR REPLACE FUNCTION fn_require_admin(p_requestor_id bigint)
RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
  IF NOT fn_is_admin(p_requestor_id) THEN
    RAISE EXCEPTION 'Недостаточно прав: требуется администратор'
      USING ERRCODE = '42501';
  END IF;
END;
$$;

-- Users

CREATE OR REPLACE FUNCTION fn_user_create(
  p_username       text,
  p_password_hash  text,
  p_password_salt  text,
  p_password_alg   text DEFAULT 'SHA-384',
  p_avatar_path    text DEFAULT NULL,
  p_role           text DEFAULT 'USER'
) RETURNS bigint
LANGUAGE plpgsql
AS $$
DECLARE v_id bigint;
BEGIN
  IF p_username IS NULL OR length(p_username) < 3 OR length(p_username) > 50 THEN
    RAISE EXCEPTION 'Некорректный username (3..50 символов)' USING ERRCODE='22023';
  END IF;

  INSERT INTO users(username, password_hash, password_salt, password_alg, avatar_path, role)
  VALUES(p_username, p_password_hash, p_password_salt, COALESCE(p_password_alg,'SHA-384'), p_avatar_path, COALESCE(p_role,'USER'))
  ON CONFLICT (username) DO NOTHING
  RETURNING id INTO v_id;

  IF v_id IS NULL THEN
    RAISE EXCEPTION 'Пользователь с username=% уже существует', p_username USING ERRCODE='23505';
  END IF;

  RETURN v_id;
END;
$$;

CREATE OR REPLACE FUNCTION fn_user_change_password(
  p_user_id   bigint,
  p_new_hash  text,
  p_new_salt  text,
  p_new_alg   text DEFAULT 'SHA-384'
) RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
  UPDATE users
  SET password_hash = p_new_hash,
      password_salt = p_new_salt,
      password_alg  = COALESCE(p_new_alg,'SHA-384')
  WHERE id = p_user_id;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Пользователь id=% не найден', p_user_id USING ERRCODE='02000';
  END IF;
END;
$$;

CREATE OR REPLACE FUNCTION fn_user_get_by_username(p_username text)
RETURNS users
LANGUAGE plpgsql
AS $$
DECLARE rec users;
BEGIN
  SELECT u.* INTO rec FROM users u WHERE u.username = p_username;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'Пользователь с username=% не найден', p_username USING ERRCODE='02000';
  END IF;
  RETURN rec;
END;
$$;

CREATE OR REPLACE FUNCTION fn_admin_set_role(
  p_requestor_id   bigint,
  p_target_user_id bigint,
  p_new_role       text
) RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE v_old_role text;
DECLARE v_admins integer;
BEGIN
  PERFORM fn_require_admin(p_requestor_id);

  SELECT role INTO v_old_role FROM users WHERE id = p_target_user_id FOR UPDATE;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'Пользователь id=% не найден', p_target_user_id USING ERRCODE='02000';
  END IF;

  IF v_old_role = 'ADMIN' AND p_new_role <> 'ADMIN' THEN
    SELECT COUNT(*) INTO v_admins FROM users WHERE role = 'ADMIN' AND id <> p_target_user_id;
    IF v_admins = 0 THEN
      RAISE EXCEPTION 'Нельзя понижать роль единственного администратора' USING ERRCODE='42501';
    END IF;
  END IF;

  UPDATE users SET role = p_new_role WHERE id = p_target_user_id;
END;
$$;

-- Albums / Tracks

CREATE OR REPLACE FUNCTION fn_album_create(
  p_name             text,
  p_description      text,
  p_cover_url        text,
  p_genre_id         bigint,
  p_track_number     integer,
  p_explicit         boolean DEFAULT false,
  p_inoagent         boolean DEFAULT false,
  p_label_id         bigint,
  p_publication_date timestamp
) RETURNS bigint
LANGUAGE plpgsql
AS $$
DECLARE v_id bigint;
BEGIN
  IF p_name IS NULL OR length(p_name) = 0 THEN
    RAISE EXCEPTION 'Название альбома не может быть пустым' USING ERRCODE='22023';
  END IF;

  PERFORM 1 FROM genre  WHERE id = p_genre_id;  IF NOT FOUND THEN RAISE EXCEPTION 'Жанр id=% не найден',  p_genre_id;  END IF;
  PERFORM 1 FROM label  WHERE id = p_label_id;  IF NOT FOUND THEN RAISE EXCEPTION 'Лейбл id=% не найден', p_label_id;  END IF;

  INSERT INTO album(name, description, cover_url, genre, track_number, explicit, inoagent, avg_rating, label_id, publication_date)
  VALUES(p_name, p_description, p_cover_url, p_genre_id, p_track_number, COALESCE(p_explicit,false), COALESCE(p_inoagent,false), 0, p_label_id, p_publication_date)
  RETURNING id INTO v_id;

  RETURN v_id;
END;
$$;

CREATE OR REPLACE FUNCTION fn_track_create(
  p_name             text,
  p_author_id        bigint,
  p_duration_sec     integer,
  p_filesize_bytes   bigint,
  p_album_id         bigint,
  p_artist_id        bigint,
  p_genre_id         bigint,
  p_filepath         text,
  p_number_on_album  integer,
  p_explicit         boolean DEFAULT false,
  p_inoagent         boolean DEFAULT false
) RETURNS bigint
LANGUAGE plpgsql
AS $$
DECLARE v_id bigint;
DECLARE v_dup int;
BEGIN
  IF p_name IS NULL OR length(p_name)=0 THEN
    RAISE EXCEPTION 'Название трека не может быть пустым' USING ERRCODE='22023';
  END IF;

  PERFORM 1 FROM users  WHERE id = p_author_id; IF NOT FOUND THEN RAISE EXCEPTION 'Автор id=% не найден',  p_author_id; END IF;
  PERFORM 1 FROM album  WHERE id = p_album_id;  IF NOT FOUND THEN RAISE EXCEPTION 'Альбом id=% не найден', p_album_id; END IF;
  PERFORM 1 FROM artist WHERE id = p_artist_id; IF NOT FOUND THEN RAISE EXCEPTION 'Артист id=% не найден', p_artist_id; END IF;
  PERFORM 1 FROM genre  WHERE id = p_genre_id;  IF NOT FOUND THEN RAISE EXCEPTION 'Жанр id=% не найден',   p_genre_id; END IF;

  SELECT COUNT(*) INTO v_dup
  FROM track WHERE album_id = p_album_id AND number_on_album = p_number_on_album;
  IF v_dup > 0 THEN
    RAISE EXCEPTION 'В альбоме % уже есть трек с номером %', p_album_id, p_number_on_album USING ERRCODE='23505';
  END IF;

  INSERT INTO track(name, author_id, duration, filesize, rating, rate_count, album_id, artist_id, genre_id, filepath, number_on_album, explicit, inoagent)
  VALUES(p_name, p_author_id, p_duration_sec, p_filesize_bytes, 0, 0, p_album_id, p_artist_id, p_genre_id, p_filepath, p_number_on_album, COALESCE(p_explicit,false), COALESCE(p_inoagent,false))
  RETURNING id INTO v_id;

  INSERT INTO artist_track(artist_id, track_id) VALUES (p_artist_id, v_id)
  ON CONFLICT DO NOTHING;

  RETURN v_id;
END;
$$;

CREATE OR REPLACE FUNCTION fn_track_update_metadata(
  p_track_id         bigint,
  p_requestor_id     bigint,
  p_name             text DEFAULT NULL,
  p_genre_id         bigint DEFAULT NULL,
  p_number_on_album  integer DEFAULT NULL,
  p_explicit         boolean DEFAULT NULL,
  p_inoagent         boolean DEFAULT NULL,
  p_filepath         text DEFAULT NULL,
  p_duration_sec     integer DEFAULT NULL,
  p_filesize_bytes   bigint DEFAULT NULL
) RETURNS track
LANGUAGE plpgsql
AS $$
DECLARE v_track track;
DECLARE v_author bigint;
DECLARE v_album  bigint;
DECLARE v_dup    int;
BEGIN
  SELECT * INTO v_track FROM track WHERE id = p_track_id FOR UPDATE;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'Трек id=% не найден', p_track_id USING ERRCODE='02000';
  END IF;

  v_author := v_track.author_id;
  v_album  := v_track.album_id;

  IF p_requestor_id <> v_author AND NOT fn_is_admin(p_requestor_id) THEN
    RAISE EXCEPTION 'Недостаточно прав менять трек id=%', p_track_id USING ERRCODE='42501';
  END IF;

  IF p_genre_id IS NOT NULL THEN
    PERFORM 1 FROM genre WHERE id = p_genre_id;
    IF NOT FOUND THEN
      RAISE EXCEPTION 'Жанр id=% не найден', p_genre_id;
    END IF;
  END IF;

  IF p_number_on_album IS NOT NULL THEN
    SELECT COUNT(*) INTO v_dup
    FROM track
    WHERE album_id = v_album AND number_on_album = p_number_on_album AND id <> p_track_id;
    IF v_dup > 0 THEN
      RAISE EXCEPTION 'В альбоме % уже есть трек с номером %', v_album, p_number_on_album USING ERRCODE='23505';
    END IF;
  END IF;

  UPDATE track t
  SET name            = COALESCE(p_name, t.name),
      genre_id        = COALESCE(p_genre_id, t.genre_id),
      number_on_album = COALESCE(p_number_on_album, t.number_on_album),
      explicit        = COALESCE(p_explicit, t.explicit),
      inoagent        = COALESCE(p_inoagent, t.inoagent),
      filepath        = COALESCE(p_filepath, t.filepath),
      duration        = COALESCE(p_duration_sec, t.duration),
      filesize        = COALESCE(p_filesize_bytes, t.filesize)
  WHERE t.id = p_track_id;

  SELECT * INTO v_track FROM track WHERE id = p_track_id;
  RETURN v_track;
END;
$$;

CREATE OR REPLACE FUNCTION fn_track_delete(
  p_track_id     bigint,
  p_requestor_id bigint
) RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE v_author bigint; DECLARE v_album bigint; DECLARE v_genre bigint; DECLARE v_label bigint;
BEGIN
  SELECT author_id, album_id, genre_id INTO v_author, v_album, v_genre
  FROM track WHERE id = p_track_id FOR UPDATE;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'Трек id=% не найден', p_track_id USING ERRCODE='02000';
  END IF;

  IF p_requestor_id <> v_author AND NOT fn_is_admin(p_requestor_id) THEN
    RAISE EXCEPTION 'Недостаточно прав удалять трек id=%', p_track_id USING ERRCODE='42501';
  END IF;

  DELETE FROM liked WHERE track_id = p_track_id;
  DELETE FROM user_rating WHERE track_id = p_track_id;
  DELETE FROM comments WHERE track_id = p_track_id;
  DELETE FROM artist_track WHERE track_id = p_track_id;
  DELETE FROM track_genre WHERE track_id = p_track_id;

  DELETE FROM track WHERE id = p_track_id;

  SELECT label_id INTO v_label FROM album WHERE id = v_album;
  PERFORM fn_recalc_album_avg(v_album);
  PERFORM fn_recalc_genre_avg(v_genre);
  IF v_label IS NOT NULL THEN PERFORM fn_recalc_label_avg(v_label); END IF;
END;
$$;

CREATE OR REPLACE FUNCTION fn_resequence_album_track_numbers(
  p_album_id bigint,
  p_order_by text DEFAULT 'number_on_album'
) RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE v_order text;
BEGIN
  IF p_order_by NOT IN ('name','upload_date','number_on_album') THEN
    v_order := 'number_on_album';
  ELSE
    v_order := p_order_by;
  END IF;

  EXECUTE format(
    'WITH s AS (
       SELECT id, ROW_NUMBER() OVER (ORDER BY %I, id) AS rn
       FROM track WHERE album_id = $1
     )
     UPDATE track t
     SET number_on_album = s.rn
     FROM s
     WHERE t.id = s.id', v_order
  ) USING p_album_id;
END;
$$;

-- Ratings

CREATE OR REPLACE FUNCTION fn_track_rate(
  p_user_id  bigint,
  p_track_id bigint,
  p_rate     smallint
) RETURNS TABLE (
  track_id    bigint,
  rate_count  integer,
  rating_sum  bigint,
  rating_avg  numeric(6,3)
)
LANGUAGE plpgsql
AS $$
BEGIN
  IF p_rate < 1 OR p_rate > 5 THEN
    RAISE EXCEPTION 'Оценка вне диапазона 1..5' USING ERRCODE='22023';
  END IF;

  PERFORM 1 FROM track WHERE id = p_track_id;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'Трек id=% не найден', p_track_id USING ERRCODE='02000';
  END IF;

  INSERT INTO user_rating(user_id, track_id, value)
  VALUES (p_user_id, p_track_id, p_rate)
  ON CONFLICT (user_id, track_id)
  DO UPDATE SET value = EXCLUDED.value;

  RETURN QUERY
  SELECT t.id,
         t.rate_count,
         t.rating,
         CASE WHEN t.rate_count > 0 THEN (t.rating::numeric / t.rate_count) ELSE NULL END
  FROM track t
  WHERE t.id = p_track_id;
END;
$$;

CREATE OR REPLACE FUNCTION fn_recalc_track_rating(
  p_track_id bigint
) RETURNS TABLE (
  track_id    bigint,
  rate_count  integer,
  rating_sum  bigint,
  rating_avg  numeric(6,3)
)
LANGUAGE plpgsql
AS $$
DECLARE v_cnt integer; DECLARE v_sum bigint;
BEGIN
  SELECT COUNT(*), COALESCE(SUM(value),0)
  INTO v_cnt, v_sum
  FROM user_rating WHERE track_id = p_track_id;

  UPDATE track
  SET rate_count = v_cnt,
      rating     = v_sum
  WHERE id = p_track_id;

  RETURN QUERY
  SELECT t.id,
         t.rate_count,
         t.rating,
         CASE WHEN t.rate_count > 0 THEN (t.rating::numeric / t.rate_count) ELSE NULL END
  FROM track t WHERE t.id = p_track_id;
END;
$$;

CREATE OR REPLACE FUNCTION fn_recalc_album_avg(p_album_id bigint)
RETURNS real
LANGUAGE plpgsql
AS $$
DECLARE v_avg numeric;
BEGIN
  SELECT AVG(r.value)::numeric INTO v_avg
  FROM user_rating r
  JOIN track t ON t.id = r.track_id
  WHERE t.album_id = p_album_id;

  UPDATE album
  SET avg_rating = COALESCE(v_avg, 0)
  WHERE id = p_album_id;

  RETURN COALESCE(v_avg, 0)::real;
END;
$$;

CREATE OR REPLACE FUNCTION fn_recalc_genre_avg(p_genre_id bigint)
RETURNS real
LANGUAGE plpgsql
AS $$
DECLARE v_avg numeric;
BEGIN
  SELECT AVG(r.value)::numeric INTO v_avg
  FROM user_rating r
  JOIN track t ON t.id = r.track_id
  WHERE t.genre_id = p_genre_id;

  UPDATE genre
  SET avg_rating = COALESCE(v_avg, 0)
  WHERE id = p_genre_id;

  RETURN COALESCE(v_avg, 0)::real;
END;
$$;

CREATE OR REPLACE FUNCTION fn_recalc_label_avg(p_label_id bigint)
RETURNS real
LANGUAGE plpgsql
AS $$
DECLARE v_avg numeric;
BEGIN
  SELECT AVG(r.value)::numeric INTO v_avg
  FROM user_rating r
  JOIN track t   ON t.id = r.track_id
  JOIN album al  ON al.id = t.album_id
  WHERE al.label_id = p_label_id;

  UPDATE label
  SET avg_rating = COALESCE(v_avg, 0)
  WHERE id = p_label_id;

  RETURN COALESCE(v_avg, 0)::real;
END;
$$;

CREATE OR REPLACE PROCEDURE proc_recalc_all_aggregates()
LANGUAGE plpgsql
AS $$
DECLARE rec record;
BEGIN
  FOR rec IN SELECT id FROM track LOOP
    PERFORM fn_recalc_track_rating(rec.id);
  END LOOP;
  FOR rec IN SELECT id FROM album LOOP
    PERFORM fn_recalc_album_avg(rec.id);
  END LOOP;
  FOR rec IN SELECT id FROM genre LOOP
    PERFORM fn_recalc_genre_avg(rec.id);
  END LOOP;
  FOR rec IN SELECT id FROM label LOOP
    PERFORM fn_recalc_label_avg(rec.id);
  END LOOP;
END;
$$;

-- Likes

CREATE OR REPLACE FUNCTION fn_like_add(p_user_id bigint, p_track_id bigint)
RETURNS boolean
LANGUAGE plpgsql
AS $$
DECLARE v_inserted boolean := false;
BEGIN
  PERFORM 1 FROM track WHERE id = p_track_id;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'Трек id=% не найден', p_track_id USING ERRCODE='02000';
  END IF;

  INSERT INTO liked(user_id, track_id, created_at)
  VALUES (p_user_id, p_track_id, now())
  ON CONFLICT (user_id, track_id) DO NOTHING;

  GET DIAGNOSTICS v_inserted = ROW_COUNT;
  RETURN v_inserted;
END;
$$;

CREATE OR REPLACE FUNCTION fn_like_remove(p_user_id bigint, p_track_id bigint)
RETURNS boolean
LANGUAGE plpgsql
AS $$
DECLARE v_deleted boolean := false;
BEGIN
  DELETE FROM liked WHERE user_id = p_user_id AND track_id = p_track_id;
  GET DIAGNOSTICS v_deleted = ROW_COUNT;
  RETURN v_deleted;
END;
$$;

CREATE OR REPLACE FUNCTION fn_user_liked_tracks(
  p_user_id bigint,
  p_limit   integer DEFAULT 20,
  p_offset  integer DEFAULT 0
) RETURNS TABLE (
  track_id     bigint,
  track_name   text,
  artist_name  text,
  album_name   text,
  liked_at     timestamp
)
LANGUAGE sql
AS $$
  SELECT t.id, t.name, ar.name, al.name, l.created_at
  FROM liked l
  JOIN track  t  ON t.id = l.track_id
  JOIN artist ar ON ar.id = t.artist_id
  JOIN album  al ON al.id = t.album_id
  WHERE l.user_id = p_user_id
  ORDER BY l.created_at DESC, t.id
  LIMIT p_limit OFFSET p_offset;
$$;

-- Comments

CREATE OR REPLACE FUNCTION fn_comment_add(
  p_user_id   bigint,
  p_track_id  bigint,
  p_body      text,
  p_track_time integer
) RETURNS comments
LANGUAGE plpgsql
AS $$
DECLARE rec comments;
BEGIN
  IF p_body IS NULL OR length(btrim(p_body)) = 0 THEN
    RAISE EXCEPTION 'Комментарий пустой' USING ERRCODE='22023';
  END IF;

  INSERT INTO comments(user_id, track_id, body, track_time, created_at)
  VALUES (p_user_id, p_track_id, p_body, p_track_time, now())
  ON CONFLICT (user_id, track_id)
  DO UPDATE SET body = EXCLUDED.body,
                track_time = EXCLUDED.track_time,
                created_at = now()
  RETURNING * INTO rec;

  RETURN rec;
END;
$$;

CREATE OR REPLACE FUNCTION fn_comment_delete(
  p_requestor_id bigint,
  p_user_id      bigint,
  p_track_id     bigint
) RETURNS boolean
LANGUAGE plpgsql
AS $$
DECLARE v_owner boolean;
DECLARE v_deleted boolean := false;
BEGIN
  v_owner := (p_requestor_id = p_user_id) OR fn_is_admin(p_requestor_id);
  IF NOT v_owner THEN
    RAISE EXCEPTION 'Недостаточно прав удалить комментарий' USING ERRCODE='42501';
  END IF;

  DELETE FROM comments WHERE user_id = p_user_id AND track_id = p_track_id;
  GET DIAGNOSTICS v_deleted = ROW_COUNT;
  RETURN v_deleted;
END;
$$;

CREATE OR REPLACE FUNCTION fn_track_comments(
  p_track_id bigint,
  p_limit    integer DEFAULT 20,
  p_offset   integer DEFAULT 0
) RETURNS TABLE (
  user_id     bigint,
  username    text,
  body        text,
  created_at  timestamp,
  track_time  integer
)
LANGUAGE sql
AS $$
  SELECT c.user_id, u.username, c.body, c.created_at, c.track_time
  FROM comments c
  JOIN users u ON u.id = c.user_id
  WHERE c.track_id = p_track_id
  ORDER BY c.created_at DESC, c.user_id
  LIMIT p_limit OFFSET p_offset;
$$;

-- Search

CREATE OR REPLACE FUNCTION fn_search_tracks(
  p_query        text DEFAULT NULL,
  p_genre_ids    bigint[] DEFAULT NULL,
  p_artist_ids   bigint[] DEFAULT NULL,
  p_label_ids    bigint[] DEFAULT NULL,
  p_explicit     boolean  DEFAULT NULL,
  p_sort         text     DEFAULT 'relevance',
  p_limit        integer  DEFAULT 20,
  p_offset       integer  DEFAULT 0
) RETURNS TABLE (
  track_id       bigint,
  track_name     text,
  artist_name    text,
  album_name     text,
  genre_title    text,
  rating_sum     bigint,
  rate_count     integer,
  rating_avg     numeric(6,3),
  publication_date timestamp,
  cover_url      text
)
LANGUAGE plpgsql
AS $$
DECLARE sql text;
DECLARE where_clauses text := 'WHERE 1=1';
DECLARE params jsonb := '{}'::jsonb;
DECLARE qpattern text;
BEGIN
  IF p_query IS NOT NULL AND btrim(p_query) <> '' THEN
    qpattern := '%' || replace(p_query, '%','\%') || '%';
    where_clauses := where_clauses || ' AND (t.name ILIKE $q OR al.name ILIKE $q OR ar.name ILIKE $q)';
    params := params || jsonb_build_object('q', qpattern);
  END IF;

  IF p_genre_ids IS NOT NULL AND array_length(p_genre_ids,1) IS NOT NULL THEN
    where_clauses := where_clauses || ' AND t.genre_id = ANY($g)';
    params := params || jsonb_build_object('g', p_genre_ids);
  END IF;

  IF p_artist_ids IS NOT NULL AND array_length(p_artist_ids,1) IS NOT NULL THEN
    where_clauses := where_clauses || ' AND t.artist_id = ANY($a)';
    params := params || jsonb_build_object('a', p_artist_ids);
  END IF;

  IF p_label_ids IS NOT NULL AND array_length(p_label_ids,1) IS NOT NULL THEN
    where_clauses := where_clauses || ' AND al.label_id = ANY($l)';
    params := params || jsonb_build_object('l', p_label_ids);
  END IF;

  IF p_explicit IS NOT NULL THEN
    where_clauses := where_clauses || ' AND t.explicit = $e';
    params := params || jsonb_build_object('e', p_explicit);
  END IF;

  sql := format($Q$
    SELECT
      t.id AS track_id,
      t.name AS track_name,
      ar.name AS artist_name,
      al.name AS album_name,
      g.name AS genre_title,
      t.rating AS rating_sum,
      t.rate_count,
      CASE WHEN t.rate_count > 0 THEN (t.rating::numeric / t.rate_count) END AS rating_avg,
      al.publication_date,
      al.cover_url
    FROM track t
    JOIN artist ar ON ar.id = t.artist_id
    JOIN album  al ON al.id = t.album_id
    JOIN genre  g  ON g.id  = t.genre_id
    %s
  $Q$, where_clauses);

  IF p_sort = 'rating' THEN
    sql := sql || ' ORDER BY (CASE WHEN t.rate_count>0 THEN t.rating::numeric/t.rate_count ELSE NULL END) DESC NULLS LAST, t.rating DESC, t.rate_count DESC, t.id';
  ELSIF p_sort = 'newest' THEN
    sql := sql || ' ORDER BY al.publication_date DESC NULLS LAST, t.id';
  ELSIF p_sort = 'popular' THEN
    sql := sql || ' ORDER BY t.rate_count DESC, t.rating DESC, t.id';
  ELSIF p_sort = 'name_asc' THEN
    sql := sql || ' ORDER BY t.name ASC, t.id';
  ELSIF p_sort = 'name_desc' THEN
    sql := sql || ' ORDER BY t.name DESC, t.id';
  ELSE
    IF params ? 'q' THEN
      sql := sql || ' ORDER BY (t.name ILIKE $q)::int DESC, (al.name ILIKE $q)::int DESC, (ar.name ILIKE $q)::int DESC, t.id';
    ELSE
      sql := sql || ' ORDER BY t.id';
    END IF;
  END IF;

  sql := sql || ' LIMIT $lim OFFSET $off';

  RETURN QUERY EXECUTE sql
    USING params->>'q', params#>'{g}', params#>'{a}', params#>'{l}', (params->>'e')::boolean, p_limit, p_offset;
END;
$$;

CREATE OR REPLACE FUNCTION fn_album_tracks(
  p_album_id bigint,
  p_sort     text    DEFAULT 'number_on_album',
  p_limit    integer DEFAULT 50,
  p_offset   integer DEFAULT 0
) RETURNS TABLE (
  track_id        bigint,
  track_name      text,
  number_on_album integer,
  rating_sum      bigint,
  rate_count      integer
)
LANGUAGE plpgsql
AS $$
DECLARE sql text;
BEGIN
  sql := '
   SELECT t.id, t.name, t.number_on_album, t.rating, t.rate_count
   FROM track t
   WHERE t.album_id = $1
  ';

  IF p_sort = 'name' THEN
    sql := sql || ' ORDER BY t.name, t.id';
  ELSIF p_sort = 'rating' THEN
    sql := sql || ' ORDER BY (CASE WHEN t.rate_count>0 THEN t.rating::numeric/t.rate_count ELSE NULL END) DESC NULLS LAST, t.rating DESC, t.id';
  ELSIF p_sort = 'newest' THEN
    sql := sql || ' ORDER BY t.upload_date DESC, t.id';
  ELSE
    sql := sql || ' ORDER BY t.number_on_album, t.id';
  END IF;

  sql := sql || ' LIMIT $2 OFFSET $3';
  RETURN QUERY EXECUTE sql USING p_album_id, p_limit, p_offset;
END;
$$;

-- Tops / Feed

CREATE OR REPLACE FUNCTION fn_top_tracks(
  p_limit   integer DEFAULT 20,
  p_metric  text    DEFAULT 'avg'
) RETURNS TABLE (
  track_id     bigint,
  track_name   text,
  artist_name  text,
  rating_sum   bigint,
  rate_count   integer,
  rating_avg   numeric(6,3)
)
LANGUAGE plpgsql
AS $$
BEGIN
  IF p_metric = 'sum' THEN
    RETURN QUERY
    SELECT t.id, t.name, ar.name, t.rating, t.rate_count,
           CASE WHEN t.rate_count>0 THEN t.rating::numeric/t.rate_count END
    FROM track t JOIN artist ar ON ar.id = t.artist_id
    ORDER BY t.rating DESC, t.rate_count DESC, t.id
    LIMIT p_limit;
  ELSE
    RETURN QUERY
    SELECT t.id, t.name, ar.name, t.rating, t.rate_count,
           CASE WHEN t.rate_count>0 THEN t.rating::numeric/t.rate_count END
    FROM track t JOIN artist ar ON ar.id = t.artist_id
    ORDER BY (CASE WHEN t.rate_count>0 THEN t.rating::numeric/t.rate_count ELSE NULL END) DESC NULLS LAST,
             t.rating DESC, t.id
    LIMIT p_limit;
  END IF;
END;
$$;

CREATE OR REPLACE FUNCTION fn_user_feed_new_tracks(
  p_user_id bigint,
  p_limit   integer DEFAULT 20
) RETURNS TABLE (
  track_id     bigint,
  track_name   text,
  artist_name  text,
  album_name   text,
  genre_title  text,
  publication_date timestamp
)
LANGUAGE sql
AS $$
WITH fav_genres AS (
  SELECT DISTINCT t.genre_id
  FROM liked l JOIN track t ON t.id = l.track_id
  WHERE l.user_id = $1
  UNION
  SELECT DISTINCT t.genre_id
  FROM user_rating r JOIN track t ON t.id = r.track_id
  WHERE r.user_id = $1
)
SELECT t.id, t.name, ar.name, al.name, g.name, al.publication_date
FROM track t
JOIN artist ar ON ar.id = t.artist_id
JOIN album  al ON al.id = t.album_id
JOIN genre  g  ON g.id = t.genre_id
WHERE t.genre_id IN (SELECT genre_id FROM fav_genres)
ORDER BY al.publication_date DESC NULLS LAST, t.id
LIMIT p_limit;
$$;

-- Admin Utils

CREATE OR REPLACE PROCEDURE proc_anonymize_user(p_user_id bigint)
LANGUAGE plpgsql
AS $$
DECLARE v_new_username text;
BEGIN
  v_new_username := 'user_' || p_user_id;
  UPDATE users
  SET username   = v_new_username,
      avatar_path = NULL
  WHERE id = p_user_id;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'Пользователь id=% не найден', p_user_id USING ERRCODE='02000';
  END IF;
END;
$$;

CREATE OR REPLACE PROCEDURE proc_delete_user(
  p_requestor_id bigint,
  p_user_id      bigint,
  p_transfer_tracks_to bigint DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
DECLARE v_cnt int;
BEGIN
  PERFORM fn_require_admin(p_requestor_id);

  IF p_transfer_tracks_to IS NOT NULL THEN
    PERFORM 1 FROM users WHERE id = p_transfer_tracks_to;
    IF NOT FOUND THEN
      RAISE EXCEPTION 'Получатель авторства id=% не найден', p_transfer_tracks_to;
    END IF;
    UPDATE track SET author_id = p_transfer_tracks_to WHERE author_id = p_user_id;
  ELSE
    SELECT COUNT(*) INTO v_cnt FROM track WHERE author_id = p_user_id;
    IF v_cnt > 0 THEN
      RAISE EXCEPTION 'У пользователя есть треки; укажите p_transfer_tracks_to' USING ERRCODE='23503';
    END IF;
  END IF;

  DELETE FROM liked       WHERE user_id = p_user_id;
  DELETE FROM user_rating WHERE user_id = p_user_id;
  DELETE FROM comments    WHERE user_id = p_user_id;

  DELETE FROM users WHERE id = p_user_id;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'Пользователь id=% не найден', p_user_id USING ERRCODE='02000';
  END IF;
END;
$$;

CREATE OR REPLACE PROCEDURE proc_compact_orphans()
LANGUAGE plpgsql
AS $$
BEGIN
  DELETE FROM liked l
  WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.id = l.user_id)
     OR NOT EXISTS (SELECT 1 FROM track t WHERE t.id = l.track_id);

  DELETE FROM user_rating r
  WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.id = r.user_id)
     OR NOT EXISTS (SELECT 1 FROM track t WHERE t.id = r.track_id);

  DELETE FROM comments c
  WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.id = c.user_id)
     OR NOT EXISTS (SELECT 1 FROM track t WHERE t.id = c.track_id);
END;
$$;

-- Triggers

CREATE OR REPLACE FUNCTION tr_user_rating_aggregate()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE v_track_id bigint;
DECLARE v_album_id bigint;
DECLARE v_genre_id bigint;
DECLARE v_label_id bigint;
BEGIN
  IF TG_OP = 'DELETE' THEN
    v_track_id := OLD.track_id;
  ELSE
    v_track_id := NEW.track_id;
  END IF;

  PERFORM fn_recalc_track_rating(v_track_id);

  SELECT t.album_id, t.genre_id INTO v_album_id, v_genre_id FROM track t WHERE t.id = v_track_id;
  IF v_album_id IS NOT NULL THEN
    PERFORM fn_recalc_album_avg(v_album_id);
    SELECT label_id INTO v_label_id FROM album WHERE id = v_album_id;
    IF v_label_id IS NOT NULL THEN
      PERFORM fn_recalc_label_avg(v_label_id);
    END IF;
  END IF;
  IF v_genre_id IS NOT NULL THEN
    PERFORM fn_recalc_genre_avg(v_genre_id);
  END IF;

  RETURN NULL;
END;
$$;

DROP TRIGGER IF EXISTS trg_user_rating_aiud ON user_rating;
CREATE TRIGGER trg_user_rating_aiud
AFTER INSERT OR UPDATE OR DELETE ON user_rating
FOR EACH ROW EXECUTE FUNCTION tr_user_rating_aggregate();

CREATE OR REPLACE FUNCTION tr_track_after_delete_recalc()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE v_label_id bigint;
BEGIN
  PERFORM fn_recalc_album_avg(OLD.album_id);
  PERFORM fn_recalc_genre_avg(OLD.genre_id);
  SELECT label_id INTO v_label_id FROM album WHERE id = OLD.album_id;
  IF v_label_id IS NOT NULL THEN
    PERFORM fn_recalc_label_avg(v_label_id);
  END IF;
  RETURN NULL;
END;
$$;

DROP TRIGGER IF EXISTS trg_track_ad ON track;
CREATE TRIGGER trg_track_ad
AFTER DELETE ON track
FOR EACH ROW EXECUTE FUNCTION tr_track_after_delete_recalc();

CREATE OR REPLACE FUNCTION tr_track_after_update_move()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE v_old_label bigint; DECLARE v_new_label bigint;
BEGIN
  IF (OLD.album_id IS DISTINCT FROM NEW.album_id) THEN
    SELECT label_id INTO v_old_label FROM album WHERE id = OLD.album_id;
    SELECT label_id INTO v_new_label FROM album WHERE id = NEW.album_id;

    PERFORM fn_recalc_album_avg(OLD.album_id);
    PERFORM fn_recalc_album_avg(NEW.album_id);
    IF v_old_label IS NOT NULL THEN PERFORM fn_recalc_label_avg(v_old_label); END IF;
    IF v_new_label IS NOT NULL THEN PERFORM fn_recalc_label_avg(v_new_label); END IF;
  END IF;

  IF (OLD.genre_id IS DISTINCT FROM NEW.genre_id) THEN
    PERFORM fn_recalc_genre_avg(OLD.genre_id);
    PERFORM fn_recalc_genre_avg(NEW.genre_id);
  END IF;

  RETURN NULL;
END;
$$;

DROP TRIGGER IF EXISTS trg_track_au_move ON track;
CREATE TRIGGER trg_track_au_move
AFTER UPDATE OF album_id, genre_id ON track
FOR EACH ROW EXECUTE FUNCTION tr_track_after_update_move();

