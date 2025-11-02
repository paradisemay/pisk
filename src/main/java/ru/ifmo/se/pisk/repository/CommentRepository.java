package ru.ifmo.se.pisk.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.ifmo.se.pisk.domain.entity.CommentEntity;
import ru.ifmo.se.pisk.domain.entity.TrackEntity;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
    List<CommentEntity> findByTrackOrderByCreatedAtAsc(TrackEntity track);

    @Modifying
    @Query("update CommentEntity c set c.complaintCount = c.complaintCount + 1 where c.id = :commentId")
    void incrementComplaintCount(@Param("commentId") Long commentId);

    Optional<CommentEntity> findFirstByTrackOrderByComplaintCountDesc(TrackEntity track);
}
