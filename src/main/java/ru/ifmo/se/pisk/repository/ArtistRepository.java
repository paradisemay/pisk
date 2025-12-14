package ru.ifmo.se.pisk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ifmo.se.pisk.domain.Artist;

public interface ArtistRepository extends JpaRepository<Artist, Long> {
}
