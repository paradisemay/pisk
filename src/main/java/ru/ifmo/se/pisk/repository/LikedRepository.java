package ru.ifmo.se.pisk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ifmo.se.pisk.domain.Liked;
import ru.ifmo.se.pisk.domain.UserTrackKey;

public interface LikedRepository extends JpaRepository<Liked, UserTrackKey> {
}
