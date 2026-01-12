
DROP FUNCTION IF EXISTS fn_recalc_track_rating(bigint) CASCADE;
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

DROP FUNCTION IF EXISTS fn_track_rate(bigint, bigint, smallint) CASCADE;
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

DROP FUNCTION IF EXISTS fn_recalc_album_avg(bigint) CASCADE;
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

DROP FUNCTION IF EXISTS fn_recalc_genre_avg(bigint) CASCADE;
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

DROP FUNCTION IF EXISTS fn_recalc_label_avg(bigint) CASCADE;
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
