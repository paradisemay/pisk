package ru.ifmo.se.pisk.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.ifmo.se.pisk.domain.UserStatus;
import ru.ifmo.se.pisk.domain.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    @Query("select u from UserEntity u where u.status = :status")
    Iterable<UserEntity> findAllByStatus(@Param("status") UserStatus status);
}
