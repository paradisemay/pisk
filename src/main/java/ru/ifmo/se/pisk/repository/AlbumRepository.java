package ru.ifmo.se.pisk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ifmo.se.pisk.repository.entity.AlbumEntity;

public interface AlbumRepository extends JpaRepository<AlbumEntity, Long> {
}
