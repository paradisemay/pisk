package ru.ifmo.se.pisk.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.ifmo.se.pisk.domain.SubscriptionStatus;
import ru.ifmo.se.pisk.domain.entity.SubscriptionEntity;
import ru.ifmo.se.pisk.domain.entity.UserEntity;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {
    Optional<SubscriptionEntity> findByFollowerAndArtist(UserEntity follower, UserEntity artist);

    long countByArtist(UserEntity artist);

    @Query("select s from SubscriptionEntity s where s.artist = :artist and s.status = :status")
    List<SubscriptionEntity> findByArtistAndStatus(@Param("artist") UserEntity artist, @Param("status") SubscriptionStatus status);
}
