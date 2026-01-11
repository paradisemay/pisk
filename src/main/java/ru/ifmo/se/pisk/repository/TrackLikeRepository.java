package ru.ifmo.se.pisk.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.ifmo.se.pisk.domain.entity.TrackEntity;
import ru.ifmo.se.pisk.domain.entity.TrackLikeEntity;
import ru.ifmo.se.pisk.domain.entity.UserEntity;

public interface TrackLikeRepository extends JpaRepository<TrackLikeEntity, Long> {
    Optional<TrackLikeEntity> findByTrackAndUser(TrackEntity track, UserEntity user);

    long countByTrack(TrackEntity track);

    Page<TrackLikeEntity> findByUser(UserEntity user, Pageable pageable);

    @org.springframework.data.jpa.repository.Query(value = "SELECT fn_like_add(:userId, :trackId)", nativeQuery = true)
    boolean likeTrack(@org.springframework.data.repository.query.Param("userId") Long userId, @org.springframework.data.repository.query.Param("trackId") Long trackId);

    @org.springframework.data.jpa.repository.Query(value = "SELECT fn_like_remove(:userId, :trackId)", nativeQuery = true)
    boolean unlikeTrack(@org.springframework.data.repository.query.Param("userId") Long userId, @org.springframework.data.repository.query.Param("trackId") Long trackId);
}
