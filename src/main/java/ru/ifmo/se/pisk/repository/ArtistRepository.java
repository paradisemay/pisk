package ru.ifmo.se.pisk.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.ifmo.se.pisk.domain.entity.ArtistEntity;

public interface ArtistRepository extends JpaRepository<ArtistEntity, Long> {

    Optional<ArtistEntity> findByNameIgnoreCase(String name);

    @Query("select a from ArtistEntity a where lower(a.name) like lower(concat('%', :namePart, '%'))")
    List<ArtistEntity> searchByName(@Param("namePart") String namePart);
}
