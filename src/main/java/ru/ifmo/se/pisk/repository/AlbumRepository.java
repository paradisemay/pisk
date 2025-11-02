package ru.ifmo.se.pisk.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.ifmo.se.pisk.domain.entity.AlbumEntity;
import ru.ifmo.se.pisk.domain.entity.UserEntity;

public interface AlbumRepository extends JpaRepository<AlbumEntity, Long> {
    List<AlbumEntity> findByOwner(UserEntity owner);

    @Query("select a from AlbumEntity a where a.releaseDate between :start and :end")
    List<AlbumEntity> findReleasedBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
