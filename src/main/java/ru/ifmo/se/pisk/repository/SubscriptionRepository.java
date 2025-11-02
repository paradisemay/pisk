package ru.ifmo.se.pisk.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.ifmo.se.pisk.repository.entity.SubscriptionEntity;
import ru.ifmo.se.pisk.repository.entity.UserEntity;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {
    Optional<SubscriptionEntity> findByFollowerAndArtist(UserEntity follower, UserEntity artist);
    long countByArtist(UserEntity artist);
}
