package ru.ifmo.se.pisk.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.ifmo.se.pisk.domain.entity.RatingEntity;
import ru.ifmo.se.pisk.domain.entity.TrackEntity;
import ru.ifmo.se.pisk.domain.entity.UserEntity;

public interface RatingRepository extends JpaRepository<RatingEntity, Long> {
    List<RatingEntity> findByTrack(TrackEntity track);
    Optional<RatingEntity> findByTrackAndUser(TrackEntity track, UserEntity user);

    @Query("select avg(r.value) from RatingEntity r where r.track = :track")
    Double calculateAverageForTrack(@Param("track") TrackEntity track);

    @Query("select count(r) from RatingEntity r where r.track = :track")
    Long countByTrack(@Param("track") TrackEntity track);
}
