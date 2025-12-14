package ru.ifmo.se.pisk.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.ifmo.se.pisk.domain.Track;

public interface TrackRepository extends JpaRepository<Track, Long> {
    List<Track> findByNameContainingIgnoreCase(String name);
}
