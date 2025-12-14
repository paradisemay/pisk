package ru.ifmo.se.pisk.service;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ifmo.se.pisk.domain.AuditAction;
import ru.ifmo.se.pisk.domain.AuditRecord;
import ru.ifmo.se.pisk.domain.entity.ArtistEntity;
import ru.ifmo.se.pisk.domain.entity.AuditEntity;
import ru.ifmo.se.pisk.domain.entity.UserEntity;
import ru.ifmo.se.pisk.domain.mapper.DomainMapper;
import ru.ifmo.se.pisk.repository.AuditRepository;

@Service
public class AuditService {

    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Transactional
    public void record(AuditAction action,
                       String entityType,
                       String entityId,
                       UserEntity actor,
                       Map<String, String> metadata) {

        AuditEntity entity = new AuditEntity();
        entity.setAction(action);
        entity.setEntityType(entityType);
        entity.setEntityId(entityId);

        entity.setUser(actor);
        entity.setArtist(null);

        if (metadata != null) {
            entity.setMetadata(metadata);
        }

        auditRepository.save(entity);
    }

    @Transactional
    public void record(AuditAction action,
                       String entityType,
                       String entityId,
                       ArtistEntity actor,
                       Map<String, String> metadata) {

        AuditEntity entity = new AuditEntity();
        entity.setAction(action);
        entity.setEntityType(entityType);
        entity.setEntityId(entityId);

        entity.setArtist(actor);
        entity.setUser(null);

        if (metadata != null) {
            entity.setMetadata(metadata);
        }

        auditRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public java.util.List<AuditRecord> findLast(int limit) {
        return auditRepository.findAll().stream()
                .sorted((a, b) -> {
                    if (a.getOccurredAt() == null && b.getOccurredAt() == null) return 0;
                    if (a.getOccurredAt() == null) return 1;
                    if (b.getOccurredAt() == null) return -1;
                    return b.getOccurredAt().compareTo(a.getOccurredAt());
                })
                .limit(limit)
                .map(DomainMapper::toAuditRecord)
                .collect(Collectors.toList());
    }
}
