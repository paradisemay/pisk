
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
  ON CONFLICT ON CONSTRAINT uk_user_rating_track_user
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
