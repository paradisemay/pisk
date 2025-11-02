package ru.ifmo.se.pisk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.ifmo.se.pisk.repository.entity.AuditEntity;

public interface AuditRepository extends JpaRepository<AuditEntity, Long> {
}
