-- V17: System Settings and Auto-delete logic

-- 1. Create System Settings Table
CREATE TABLE system_settings (
    id INT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    moderation_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    default_page_size INT NOT NULL DEFAULT 10,
    track_deletion_threshold NUMERIC(3, 2) DEFAULT 0 -- 0 means disabled
);

INSERT INTO system_settings(id, moderation_enabled, default_page_size, track_deletion_threshold)
VALUES (1, TRUE, 10, 0)
ON CONFLICT DO NOTHING;

-- 2. Internal delete function (bypasses permission checks)
CREATE OR REPLACE FUNCTION fn_track_delete_internal(p_track_id bigint)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE v_album bigint; DECLARE v_genre bigint;
BEGIN
  -- Get info for recalc later (if needed, though trigger does it)
  SELECT album_id, genre_id INTO v_album, v_genre
  FROM track WHERE id = p_track_id FOR UPDATE;

  IF NOT FOUND THEN RETURN; END IF;

  DELETE FROM liked WHERE track_id = p_track_id;
  DELETE FROM user_rating WHERE track_id = p_track_id;
  DELETE FROM comments WHERE track_id = p_track_id;
  DELETE FROM artist_track WHERE track_id = p_track_id;
  DELETE FROM track_genre WHERE track_id = p_track_id;

  -- Handle V11 table if exists (using dynamic sql to avoid error if table missing in dev? No, V11 is applied)
  DELETE FROM track_status_history WHERE track_id = p_track_id;

  DELETE FROM track WHERE id = p_track_id;
END;
$$;

-- 3. Refined public delete function
CREATE OR REPLACE FUNCTION fn_track_delete(
  p_track_id     bigint,
  p_requestor_id bigint
) RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE v_author bigint;
BEGIN
  SELECT author_id INTO v_author FROM track WHERE id = p_track_id;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Трек id=% не найден', p_track_id USING ERRCODE='02000';
  END IF;

  IF p_requestor_id <> v_author AND NOT fn_is_admin(p_requestor_id) THEN
    RAISE EXCEPTION 'Недостаточно прав удалять трек id=%', p_track_id USING ERRCODE='42501';
  END IF;

  PERFORM fn_track_delete_internal(p_track_id);
END;
$$;

-- 4. Refined recalc function with auto-delete
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
DECLARE v_cnt integer;
DECLARE v_sum bigint;
DECLARE v_avg numeric;
DECLARE v_thresh numeric;
BEGIN
  SELECT COUNT(*), COALESCE(SUM(rate),0)
  INTO v_cnt, v_sum
  FROM user_rating WHERE track_id = p_track_id;

  UPDATE track
  SET rate_count = v_cnt,
      rating     = v_sum
  WHERE id = p_track_id;

  -- Auto-delete check
  IF v_cnt > 0 THEN
      v_avg := v_sum::numeric / v_cnt;

      SELECT track_deletion_threshold INTO v_thresh FROM system_settings WHERE id = 1;

      IF v_thresh IS NOT NULL AND v_thresh > 0 AND v_avg < v_thresh THEN
          PERFORM fn_track_delete_internal(p_track_id);
          RETURN; -- Return empty result as track is deleted
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
