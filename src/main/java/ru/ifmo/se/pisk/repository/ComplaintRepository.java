package ru.ifmo.se.pisk.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.ifmo.se.pisk.domain.ComplaintStatus;
import ru.ifmo.se.pisk.domain.entity.ComplaintEntity;
import ru.ifmo.se.pisk.domain.entity.TrackEntity;
import ru.ifmo.se.pisk.domain.entity.UserEntity;

public interface ComplaintRepository extends JpaRepository<ComplaintEntity, Long> {
    List<ComplaintEntity> findByStatus(ComplaintStatus status);

    boolean existsByTrackAndComplainantAndStatus(TrackEntity track, UserEntity complainant, ComplaintStatus status);

    @Query("select count(c) from ComplaintEntity c where c.track = :track and c.status = :status")
    long countByTrackAndStatus(@Param("track") TrackEntity track, @Param("status") ComplaintStatus status);

    Optional<ComplaintEntity> findFirstByTrackOrderByCreatedAtAsc(TrackEntity track);
}
