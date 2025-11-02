package ru.ifmo.se.pisk.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.ifmo.se.pisk.repository.entity.RatingEntity;
import ru.ifmo.se.pisk.repository.entity.TrackEntity;
import ru.ifmo.se.pisk.repository.entity.UserEntity;

public interface RatingRepository extends JpaRepository<RatingEntity, Long> {
    List<RatingEntity> findByTrack(TrackEntity track);
    Optional<RatingEntity> findByTrackAndUser(TrackEntity track, UserEntity user);
}
