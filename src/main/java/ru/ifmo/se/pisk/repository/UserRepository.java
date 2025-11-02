package ru.ifmo.se.pisk.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.ifmo.se.pisk.repository.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByUsername(String username);
}
