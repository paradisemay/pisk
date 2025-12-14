package ru.ifmo.se.pisk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ifmo.se.pisk.domain.Album;

public interface AlbumRepository extends JpaRepository<Album, Long> {
}
