package ru.ifmo.se.pisk.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.ifmo.se.pisk.domain.entity.GenreEntity;

public interface GenreRepository extends JpaRepository<GenreEntity, Long> {
    Optional<GenreEntity> findByNameIgnoreCase(String name);

    @Query("select g from GenreEntity g where lower(g.name) like lower(concat('%', :namePart, '%'))")
    List<GenreEntity> searchByName(@Param("namePart") String namePart);
}
