package ru.ifmo.se.pisk.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.ifmo.se.pisk.domain.UserRating;
import ru.ifmo.se.pisk.domain.UserTrackKey;

public interface UserRatingRepository extends JpaRepository<UserRating, UserTrackKey> {
    Optional<UserRating> findByUserIdAndTrackId(Long userId, Long trackId);
}
