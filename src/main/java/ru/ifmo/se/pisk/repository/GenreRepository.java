package ru.ifmo.se.pisk.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.ifmo.se.pisk.domain.Genre;

public interface GenreRepository extends JpaRepository<Genre, Long> {
    Optional<Genre> findByTitle(String title);
}
