package ru.ifmo.se.pisk.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.ifmo.se.pisk.domain.TrackStatus;
import ru.ifmo.se.pisk.repository.entity.TrackEntity;
import ru.ifmo.se.pisk.repository.entity.UserEntity;

public interface TrackRepository extends JpaRepository<TrackEntity, Long>, JpaSpecificationExecutor<TrackEntity> {
    Page<TrackEntity> findByStatus(TrackStatus status, Pageable pageable);
    Page<TrackEntity> findByArtist(UserEntity artist, Pageable pageable);
    Optional<TrackEntity> findByIdAndArtist(Long id, UserEntity artist);
}
