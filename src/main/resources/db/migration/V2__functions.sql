-- ===================================================================
-- Хранимые функции/процедуры и триггеры для музыкальной платформы
-- Схема: как в твоём DDL (users, artist, album, track, genre, label,
--        liked, user_rating, comments, artist_track, track_genre)
-- PostgreSQL 15+, язык plpgsql
-- ===================================================================

-- На всякий случай (обычно уже есть)
-- CREATE EXTENSION IF NOT EXISTS plpgsql;

-- ---------------------------------------------------------------
-- УТИЛИТЫ / ПОМОЩНИКИ
-- ---------------------------------------------------------------

-- Проверка: является ли пользователь админом (users.role = 1)
CREATE OR REPLACE FUNCTION fn_is_admin(p_user_id bigint)
RETURNS boolean
LANGUAGE sql
AS $$
  SELECT EXISTS (
    SELECT 1 FROM users u
    WHERE u.id = p_user_id AND u.role = 1
  );
$$;

-- Маленький helper: бросить ошибку, если не админ
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

-- ---------------------------------------------------------------
-- A) ПОЛЬЗОВАТЕЛИ И РОЛИ
-- ---------------------------------------------------------------

-- Создать пользователя (hash/salt/alg вычислены в приложении)
CREATE OR REPLACE FUNCTION fn_user_create(
  p_username       text,
  p_password_hash  bytea,
  p_password_salt  bytea,
  p_password_alg   text DEFAULT 'SHA-384',
  p_avatar_path    text DEFAULT NULL,
  p_role           smallint DEFAULT 0
) RETURNS bigint
LANGUAGE plpgsql
AS $$
DECLARE v_id bigint;
BEGIN
  IF p_username IS NULL OR length(p_username) < 3 OR length(p_username) > 50 THEN
    RAISE EXCEPTION 'Некорректный username (3..50 символов)' USING ERRCODE='22023';
  END IF;

  INSERT INTO users(username, password_hash, password_salt, password_alg, avatar_path, role)
  VALUES(p_username, p_password_hash, p_password_salt, COALESCE(p_password_alg,'SHA-384'), p_avatar_path, COALESCE(p_role,0))
  ON CONFLICT (username) DO NOTHING
  RETURNING id INTO v_id;

  IF v_id IS NULL THEN
    RAISE EXCEPTION 'Пользователь с username=% уже существует', p_username USING ERRCODE='23505';
  END IF;

  RETURN v_id;
END;
$$;

-- Смена пароля
CREATE OR REPLACE FUNCTION fn_user_change_password(
  p_user_id   bigint,
  p_new_hash  bytea,
  p_new_salt  bytea,
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

-- Получить пользователя по нику
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

-- Админ: сменить роль пользователю (не давать удалить последнего админа)
CREATE OR REPLACE FUNCTION fn_admin_set_role(
  p_requestor_id   bigint,
  p_target_user_id bigint,
  p_new_role       smallint
) RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE v_old_role smallint;
DECLARE v_admins integer;
BEGIN
  PERFORM fn_require_admin(p_requestor_id);

  SELECT role INTO v_old_role FROM users WHERE id = p_target_user_id FOR UPDATE;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'Пользователь id=% не найден', p_target_user_id USING ERRCODE='02000';
  END IF;

  IF v_old_role = 1 AND p_new_role <> 1 THEN
    SELECT COUNT(*) INTO v_admins FROM users WHERE role = 1 AND id <> p_target_user_id;
    IF v_admins = 0 THEN
      RAISE EXCEPTION 'Нельзя понижать роль единственного администратора' USING ERRCODE='42501';
    END IF;
  END IF;

  UPDATE users SET role = p_new_role WHERE id = p_target_user_id;
END;
$$;

-- ---------------------------------------------------------------
-- B) АЛЬБОМЫ / ТРЕКИ (CRUD)
-- ---------------------------------------------------------------

-- Создать альбом
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

-- Создать трек
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

  -- Проверка уникальности номера в альбоме (т.к. в схеме нет индекса)
  SELECT COUNT(*) INTO v_dup
  FROM track WHERE album_id = p_album_id AND number_on_album = p_number_on_album;
  IF v_dup > 0 THEN
    RAISE EXCEPTION 'В альбоме % уже есть трек с номером %', p_album_id, p_number_on_album USING ERRCODE='23505';
  END IF;

  INSERT INTO track(name, author_id, duration, filesize, rating, rate_count, album_id, artist_id, genre_id, filepath, number_on_album, explicit, inoagent)
  VALUES(p_name, p_author_id, p_duration_sec, p_filesize_bytes, 0, 0, p_album_id, p_artist_id, p_genre_id, p_filepath, p_number_on_album, COALESCE(p_explicit,false), COALESCE(p_inoagent,false))
  RETURNING id INTO v_id;

  -- Привязка в artist_track (1:1 основного)
  INSERT INTO artist_track(artist_id, track_id) VALUES (p_artist_id, v_id)
  ON CONFLICT DO NOTHING;

  RETURN v_id;
END;
$$;

-- Обновление метаданных трека (только автор или админ)
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

-- Удалить трек (автор или админ)
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

  -- Сносим зависимые (PK/FK каскадов нет, поэтому вручную)
  DELETE FROM liked WHERE track_id = p_track_id;
  DELETE FROM user_rating WHERE track_id = p_track_id;
  DELETE FROM comments WHERE track_id = p_track_id;
  DELETE FROM artist_track WHERE track_id = p_track_id;
  DELETE FROM track_genre WHERE track_id = p_track_id;

  DELETE FROM track WHERE id = p_track_id;

  -- После удаления пересчитаем агрегаты альбома/жанра/лейбла
  SELECT label_id INTO v_label FROM album WHERE id = v_album;
  PERFORM fn_recalc_album_avg(v_album);
  PERFORM fn_recalc_genre_avg(v_genre);
  IF v_label IS NOT NULL THEN PERFORM fn_recalc_label_avg(v_label); END IF;
END;
$$;

-- Перенумеровать треки в альбоме
CREATE OR REPLACE FUNCTION fn_resequence_album_track_numbers(
  p_album_id bigint,
  p_order_by text DEFAULT 'number_on_album' -- 'name'|'upload_date'|'number_on_album'
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

  -- Динамически сортируем и ставим row_number()
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

-- ---------------------------------------------------------------
-- C) РЕЙТИНГИ И АГРЕГАТЫ
-- ---------------------------------------------------------------

-- Поставить/обновить оценку (триггеры пересчитают агрегаты)
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

  -- Убедимся, что трек существует
  PERFORM 1 FROM track WHERE id = p_track_id;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'Трек id=% не найден', p_track_id USING ERRCODE='02000';
  END IF;

  INSERT INTO user_rating(user_id, track_id, rate)
  VALUES (p_user_id, p_track_id, p_rate)
  ON CONFLICT (user_id, track_id)
  DO UPDATE SET rate = EXCLUDED.rate;

  -- Триггер уже обновил track.*; возвращаем агрегаты
  RETURN QUERY
  SELECT t.id,
         t.rate_count,
         t.rating,
         CASE WHEN t.rate_count > 0 THEN (t.rating::numeric / t.rate_count) ELSE NULL END
  FROM track t
  WHERE t.id = p_track_id;
END;
$$;

-- Полный пересчёт агрегатов одного трека (из user_rating)
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
  SELECT COUNT(*), COALESCE(SUM(rate),0)
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

-- Средняя по альбому (по всем оценкам его треков)
CREATE OR REPLACE FUNCTION fn_recalc_album_avg(p_album_id bigint)
RETURNS real
LANGUAGE plpgsql
AS $$
DECLARE v_avg numeric;
BEGIN
  SELECT AVG(r.rate)::numeric INTO v_avg
  FROM user_rating r
  JOIN track t ON t.id = r.track_id
  WHERE t.album_id = p_album_id;

  UPDATE album
  SET avg_rating = COALESCE(v_avg, 0)
  WHERE id = p_album_id;

  RETURN COALESCE(v_avg, 0)::real;
END;
$$;

-- Средняя по жанру
CREATE OR REPLACE FUNCTION fn_recalc_genre_avg(p_genre_id bigint)
RETURNS real
LANGUAGE plpgsql
AS $$
DECLARE v_avg numeric;
BEGIN
  SELECT AVG(r.rate)::numeric INTO v_avg
  FROM user_rating r
  JOIN track t ON t.id = r.track_id
  WHERE t.genre_id = p_genre_id;

  UPDATE genre
  SET avg_rating = COALESCE(v_avg, 0)
  WHERE id = p_genre_id;

  RETURN COALESCE(v_avg, 0)::real;
END;
$$;

-- Средняя по лейблу
CREATE OR REPLACE FUNCTION fn_recalc_label_avg(p_label_id bigint)
RETURNS real
LANGUAGE plpgsql
AS $$
DECLARE v_avg numeric;
BEGIN
  SELECT AVG(r.rate)::numeric INTO v_avg
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

-- Пакетный пересчёт всех агрегатов (ночной/админ)
CREATE OR REPLACE PROCEDURE proc_recalc_all_aggregates()
LANGUAGE plpgsql
AS $$
DECLARE rec record;
BEGIN
  -- Треки
  FOR rec IN SELECT id FROM track LOOP
    PERFORM fn_recalc_track_rating(rec.id);
  END LOOP;

  -- Альбомы
  FOR rec IN SELECT id FROM album LOOP
    PERFORM fn_recalc_album_avg(rec.id);
  END LOOP;

  -- Жанры
  FOR rec IN SELECT id FROM genre LOOP
    PERFORM fn_recalc_genre_avg(rec.id);
  END LOOP;

  -- Лейблы
  FOR rec IN SELECT id FROM label LOOP
    PERFORM fn_recalc_label_avg(rec.id);
  END LOOP;
END;
$$;

-- ---------------------------------------------------------------
-- D) ЛАЙКИ
-- ---------------------------------------------------------------

-- Добавить лайк (идемпотентно)
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

-- Удалить лайк (идемпотентно)
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

-- Лайкнутые треки пользователя
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

-- ---------------------------------------------------------------
-- E) КОММЕНТАРИИ
-- ---------------------------------------------------------------

-- Добавить/обновить комментарий (по твоему PK один комментарий на пару user/track)
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

-- Удалить комментарий (владелец или админ)
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
  -- владелец?
  v_owner := (p_requestor_id = p_user_id) OR fn_is_admin(p_requestor_id);
  IF NOT v_owner THEN
    RAISE EXCEPTION 'Недостаточно прав удалить комментарий' USING ERRCODE='42501';
  END IF;

  DELETE FROM comments WHERE user_id = p_user_id AND track_id = p_track_id;
  GET DIAGNOSTICS v_deleted = ROW_COUNT;
  RETURN v_deleted;
END;
$$;

-- Комментарии трека
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

-- ---------------------------------------------------------------
-- F) ПОИСК / КАТАЛОГ
-- ---------------------------------------------------------------

-- Поиск треков с фильтрами и сортировкой (простой ILIKE; без FTS)
CREATE OR REPLACE FUNCTION fn_search_tracks(
  p_query        text DEFAULT NULL,
  p_genre_ids    bigint[] DEFAULT NULL,
  p_artist_ids   bigint[] DEFAULT NULL,
  p_label_ids    bigint[] DEFAULT NULL,
  p_explicit     boolean  DEFAULT NULL,  -- NULL = без фильтра
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
  -- WHERE
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

  -- ORDER BY
  -- relevance -> по совпадению имени трека/альбома/артиста, затем средняя оценка
  -- rating    -> по среднему рейтингу (sum/count), затем сумме/кол-ву
  -- newest    -> по дате публикации альбома
  -- popular   -> по rate_count
  -- name_asc/name_desc
  sql := format($Q$
    SELECT
      t.id AS track_id,
      t.name AS track_name,
      ar.name AS artist_name,
      al.name AS album_name,
      g.title AS genre_title,
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
    -- relevance
    IF params ? 'q' THEN
      sql := sql || ' ORDER BY (t.name ILIKE $q)::int DESC, (al.name ILIKE $q)::int DESC, (ar.name ILIKE $q)::int DESC, t.id';
    ELSE
      sql := sql || ' ORDER BY t.id';
    END IF;
  END IF;

  sql := sql || ' LIMIT $lim OFFSET $off';

  -- EXECUTE с подстановкой параметров
  RETURN QUERY EXECUTE sql
    USING params->>'q', params#>'{g}', params#>'{a}', params#>'{l}', (params->>'e')::boolean, p_limit, p_offset;
END;
$$;

-- Треки альбома (сортировка)
CREATE OR REPLACE FUNCTION fn_album_tracks(
  p_album_id bigint,
  p_sort     text    DEFAULT 'number_on_album',  -- 'name'|'rating'|'newest'
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

-- ---------------------------------------------------------------
-- G) ТОПЫ / ФИД
-- ---------------------------------------------------------------

-- Топ треков (all-time), p_metric: 'avg' | 'sum'
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

-- "Лента" по жанрам, которые юзер лайкал или оценивал; сортировка по дате релиза альбома
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
SELECT t.id, t.name, ar.name, al.name, g.title, al.publication_date
FROM track t
JOIN artist ar ON ar.id = t.artist_id
JOIN album  al ON al.id = t.album_id
JOIN genre  g  ON g.id = t.genre_id
WHERE t.genre_id IN (SELECT genre_id FROM fav_genres)
ORDER BY al.publication_date DESC NULLS LAST, t.id
LIMIT p_limit;
$$;

-- ---------------------------------------------------------------
-- H) АДМИН-УТИЛИТЫ
-- ---------------------------------------------------------------

-- Анонимизация пользователя
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

-- Удаление пользователя (с возможной передачей авторства треков)
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
    -- Проверим получателя
    PERFORM 1 FROM users WHERE id = p_transfer_tracks_to;
    IF NOT FOUND THEN
      RAISE EXCEPTION 'Получатель авторства id=% не найден', p_transfer_tracks_to;
    END IF;
    UPDATE track SET author_id = p_transfer_tracks_to WHERE author_id = p_user_id;
  ELSE
    -- Нельзя удалять автора, если у него есть треки
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

-- Чистка "висящих" ссылок
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

-- ---------------------------------------------------------------
-- ТРИГГЕРЫ ДЛЯ АВТО-АГРЕГАЦИЙ
-- ---------------------------------------------------------------

-- Триггер-функция: после INSERT/UPDATE/DELETE в user_rating — пересчитать
-- track.rate_count/rating и обновить avg альбома/жанра/лейбла
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

  -- Пересчёт трека
  PERFORM fn_recalc_track_rating(v_track_id);

  -- Сопутствующие агрегаты
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

-- Привязка триггера к user_rating
DROP TRIGGER IF EXISTS trg_user_rating_aiud ON user_rating;
CREATE TRIGGER trg_user_rating_aiud
AFTER INSERT OR UPDATE OR DELETE ON user_rating
FOR EACH ROW EXECUTE FUNCTION tr_user_rating_aggregate();

-- Триггер при УДАЛЕНИИ трека: обновить агрегаты альбома/жанра/лейбла
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

-- Триггер при переносе трека между альбомами/жанрами: пересчитать старые и новые
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

-- ===================================================================
-- КОНЕЦ НАБОРА
-- ===================================================================

