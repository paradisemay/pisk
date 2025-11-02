package ru.ifmo.se.pisk.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.ifmo.se.pisk.domain.entity.TrackEntity;
import ru.ifmo.se.pisk.domain.entity.TrackLikeEntity;
import ru.ifmo.se.pisk.domain.entity.UserEntity;

public interface TrackLikeRepository extends JpaRepository<TrackLikeEntity, Long> {
    Optional<TrackLikeEntity> findByTrackAndUser(TrackEntity track, UserEntity user);

    long countByTrack(TrackEntity track);
}
