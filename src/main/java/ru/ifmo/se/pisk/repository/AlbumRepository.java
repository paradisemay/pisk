package ru.ifmo.se.pisk.repository;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.ifmo.se.pisk.domain.entity.AlbumEntity;
import ru.ifmo.se.pisk.domain.entity.LabelEntity;

public interface AlbumRepository extends JpaRepository<AlbumEntity, Long> {
    List<AlbumEntity> findByLabel(LabelEntity label);

    @Query("select a from AlbumEntity a where a.publicationDate between :start and :end")
    List<AlbumEntity> findReleasedBetween(@Param("start") Instant start, @Param("end") Instant end);
}
