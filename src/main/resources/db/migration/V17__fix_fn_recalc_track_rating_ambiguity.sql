-- Fix ambiguous column reference in fn_recalc_track_rating and ensure 'value' column usage
-- Also update other functions that might be affected by 'rate' -> 'value' rename

-- 1. Fix fn_recalc_track_rating
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
  -- Use explicit alias 'ur' to avoid ambiguity with output parameter 'track_id'
  -- Use 'value' column instead of 'rate'
  SELECT COUNT(*), COALESCE(SUM(ur.value),0)
  INTO v_cnt, v_sum
  FROM user_rating ur WHERE ur.track_id = p_track_id;

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

-- 2. Update fn_track_rate to use 'value' and explicit aliases
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

  -- Use 'value' column
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

-- 3. Update fn_recalc_album_avg to use 'value'
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

-- 4. Update fn_recalc_genre_avg to use 'value'
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

-- 5. Update fn_recalc_label_avg to use 'value'
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
