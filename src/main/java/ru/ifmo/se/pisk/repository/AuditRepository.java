package ru.ifmo.se.pisk.repository;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.ifmo.se.pisk.domain.AuditAction;
import ru.ifmo.se.pisk.domain.entity.AuditEntity;

public interface AuditRepository extends JpaRepository<AuditEntity, Long> {
    @Query("select a from AuditEntity a where a.action = :action and a.occurredAt >= :from order by a.occurredAt desc")
    List<AuditEntity> findRecentByAction(@Param("action") AuditAction action, @Param("from") Instant from);
}
