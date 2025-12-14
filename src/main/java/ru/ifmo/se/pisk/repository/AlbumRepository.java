package ru.ifmo.se.pisk.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.ifmo.se.pisk.domain.entity.AlbumEntity;
import ru.ifmo.se.pisk.domain.entity.LabelEntity;

public interface AlbumRepository extends JpaRepository<AlbumEntity, Long> {
    List<AlbumEntity> findByLabel(LabelEntity label);

    @Query("select a from AlbumEntity a where a.publicationDate between :start and :end")
    List<AlbumEntity> findReleasedBetween(@Param("start") Instant start, @Param("end") Instant end);

    Optional<AlbumEntity> findByNameIgnoreCase(String name);

    @Query("select a from AlbumEntity a where lower(a.name) like lower(concat('%', :namePart, '%'))")
    List<AlbumEntity> searchByName(@Param("namePart") String namePart);
}
