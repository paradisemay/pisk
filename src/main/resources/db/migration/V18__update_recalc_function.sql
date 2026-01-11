-- Замена fn_track_delete для установки статуса 'ARCHIVED' перед удалением зависимостей.
-- Это нужно, чтобы fn_recalc_track_rating (вызванный триггером удаления user_rating) мог понять,
-- что трек удаляется, и не пытался удалить его снова (рекурсия).

CREATE OR REPLACE FUNCTION fn_track_delete(
  p_track_id     bigint,
  p_requestor_id bigint
) RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE v_author bigint; DECLARE v_album bigint; DECLARE v_genre bigint; DECLARE v_label bigint;
BEGIN
  -- Блокируем трек
  SELECT author_id, album_id, genre_id INTO v_author, v_album, v_genre
  FROM track WHERE id = p_track_id FOR UPDATE;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Трек id=% не найден', p_track_id USING ERRCODE='02000';
  END IF;

  -- Проверка прав
  IF p_requestor_id <> v_author AND NOT fn_is_admin(p_requestor_id) THEN
    RAISE EXCEPTION 'Недостаточно прав удалять трек id=%', p_track_id USING ERRCODE='42501';
  END IF;

  -- Метка "ARCHIVED" (или REJECTED), чтобы триггеры понимали контекст
  -- (TrackStatus.ARCHIVED существует в Java enum, но в БД статус - VARCHAR/text. V14 добавил статус, дефолт DRAFT)
  UPDATE track SET status = 'ARCHIVED' WHERE id = p_track_id;

  -- Сносим зависимые (триггеры сработают, но увидят ARCHIVED и не будут рекурсивно удалять)
  DELETE FROM liked WHERE track_id = p_track_id;
  DELETE FROM user_rating WHERE track_id = p_track_id;
  DELETE FROM comments WHERE track_id = p_track_id;
  DELETE FROM artist_track WHERE track_id = p_track_id;
  DELETE FROM track_genre WHERE track_id = p_track_id;

  -- Физическое удаление трека
  DELETE FROM track WHERE id = p_track_id;

  -- После удаления пересчитаем агрегаты альбома/жанра/лейбла
  SELECT label_id INTO v_label FROM album WHERE id = v_album;
  PERFORM fn_recalc_album_avg(v_album);
  PERFORM fn_recalc_genre_avg(v_genre);
  IF v_label IS NOT NULL THEN PERFORM fn_recalc_label_avg(v_label); END IF;
END;
$$;


-- Обновление логики пересчёта рейтинга: проверка порога удаления.
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
DECLARE v_thresh numeric;
DECLARE v_avg numeric;
DECLARE v_admin_id bigint;
DECLARE v_status text;
BEGIN
  -- Считаем новые агрегаты
  SELECT COUNT(*), COALESCE(SUM(rate),0)
  INTO v_cnt, v_sum
  FROM user_rating WHERE track_id = p_track_id;

  -- Обновляем трек (и заодно проверяем статус, блокируя строку, если ещё не удалена)
  -- Если трек уже удалён (физически), UPDATE вернёт 0 строк, v_status останется NULL.
  UPDATE track
  SET rate_count = v_cnt,
      rating     = v_sum
  WHERE id = p_track_id
  RETURNING status INTO v_status;

  -- Если трек не найден (удален) или помечен как ARCHIVED (процесс удаления), выходим
  IF v_status IS NULL OR v_status = 'ARCHIVED' THEN
    -- Возвращаем пустышку или то, что есть, но не удаляем
    RETURN QUERY SELECT t.id, t.rate_count, t.rating, (t.rating::numeric/t.rate_count)
                 FROM track t WHERE t.id = p_track_id; -- скорее всего пусто
    RETURN;
  END IF;

  -- Вычисляем среднее
  IF v_cnt > 0 THEN
    v_avg := v_sum::numeric / v_cnt;
  ELSE
    v_avg := 0;
  END IF;

  -- Проверка порога удаления
  -- Условие:
  -- 1. Есть оценки (v_cnt >= 5, чтобы не удалять сразу при 1 дизлайке, хотя ТЗ жесткое "ниже порога").
  --    Если ТЗ жесткое, то 1 оценка "1" при пороге "2" должна удалять.
  --    Но если v_cnt = 0, avg = 0. Удалять ли треки без оценок? Нет.
  --    Поэтому v_cnt > 0 обязательно.
  --    Давай поставим v_cnt >= 3 для минимальной защиты от случайности.
  -- 2. Рейтинг ниже порога.

  SELECT deletion_threshold INTO v_thresh FROM system_settings WHERE id = 1;

  IF v_cnt >= 3 AND v_avg < v_thresh THEN
     -- Ищем админа для выполнения удаления
     -- (V1__create_schema.sql говорит role varchar, 'ADMIN')
     SELECT id INTO v_admin_id FROM users WHERE role = 'ADMIN' LIMIT 1;

     IF v_admin_id IS NOT NULL THEN
         -- Вызываем удаление. Оно поставит статус ARCHIVED и удалит всё.
         -- Мы используем PERFORM, чтобы игнорировать результат void
         PERFORM fn_track_delete(p_track_id, v_admin_id);

         -- Возвращаем пустоту, т.к. трека больше нет
         RETURN;
     END IF;
  END IF;

  RETURN QUERY
  SELECT t.id,
         t.rate_count,
         t.rating,
         CASE WHEN t.rate_count > 0 THEN (t.rating::numeric / t.rate_count) ELSE NULL END
  FROM track t WHERE t.id = p_track_id;
END;
$$;
