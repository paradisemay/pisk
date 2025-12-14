package ru.ifmo.se.pisk.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.ifmo.se.pisk.domain.Role;
import ru.ifmo.se.pisk.domain.RoleType;

public interface RoleRepository extends JpaRepository<Role, Short> {
    Optional<Role> findByRole(RoleType roleType);
}
