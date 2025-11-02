package ru.ifmo.se.pisk.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ifmo.se.pisk.domain.AuditAction;
import ru.ifmo.se.pisk.domain.ComplaintInfo;
import ru.ifmo.se.pisk.domain.ComplaintStatus;
import ru.ifmo.se.pisk.domain.TrackStatus;
import ru.ifmo.se.pisk.domain.entity.ComplaintEntity;
import ru.ifmo.se.pisk.domain.entity.TrackEntity;
import ru.ifmo.se.pisk.domain.entity.UserEntity;
import ru.ifmo.se.pisk.domain.mapper.DomainMapper;
import ru.ifmo.se.pisk.repository.ComplaintRepository;
import ru.ifmo.se.pisk.service.dto.ComplaintDecisionCommand;

@Service
public class ComplaintService {

    private static final int TRACK_COMPLAINT_THRESHOLD = 5;

    private final ComplaintRepository complaintRepository;
    private final TrackService trackService;
    private final UserService userService;
    private final AuditService auditService;

    public ComplaintService(ComplaintRepository complaintRepository,
                            TrackService trackService,
                            UserService userService,
                            AuditService auditService) {
        this.complaintRepository = complaintRepository;
        this.trackService = trackService;
        this.userService = userService;
        this.auditService = auditService;
    }

    @Transactional
    public ComplaintInfo createComplaint(Long trackId, Long complainantId, String reason) {
        TrackEntity track = trackService.requireTrack(trackId);
        UserEntity complainant = userService.requireUser(complainantId);
        if (complaintRepository.existsByTrackAndComplainantAndStatus(track, complainant, ComplaintStatus.OPEN)) {
            throw new IllegalStateException("Уже есть открытая жалоба от пользователя на этот трек");
        }
        ComplaintEntity entity = new ComplaintEntity();
        entity.setTrack(track);
        entity.setComplainant(complainant);
        entity.setReason(reason);
        entity.setStatus(ComplaintStatus.OPEN);
        ComplaintEntity saved = complaintRepository.save(entity);
        long openComplaints = complaintRepository.countByTrackAndStatus(track, ComplaintStatus.OPEN);
        if (openComplaints >= TRACK_COMPLAINT_THRESHOLD && track.getStatus() == TrackStatus.PUBLISHED) {
            trackService.changeStatus(track.getId(), TrackStatus.PENDING_REVIEW, complainantId);
        }
        auditService.record(AuditAction.COMPLAINT_FILED, "Complaint", saved.getId().toString(), complainantId,
                Map.of("trackId", trackId.toString()));
        return DomainMapper.toComplaintInfo(saved);
    }

    @Transactional
    public ComplaintInfo decideComplaint(ComplaintDecisionCommand command) {
        ComplaintEntity entity = complaintRepository.findById(command.complaintId())
                .orElseThrow(() -> new IllegalArgumentException("Жалоба не найдена"));
        entity.setStatus(command.targetStatus());
        entity.setModeratorNote(command.moderatorNote());
        entity.setResolvedAt(Instant.now());
        if (command.targetStatus() == ComplaintStatus.RESOLVED) {
            trackService.changeStatus(entity.getTrack().getId(), TrackStatus.ARCHIVED, command.moderatorId());
        }
        auditService.record(AuditAction.COMPLAINT_RESOLVED, "Complaint", entity.getId().toString(), command.moderatorId(),
                Map.of("status", command.targetStatus().name()));
        return DomainMapper.toComplaintInfo(entity);
    }

    @Transactional(readOnly = true)
    public List<ComplaintInfo> listOpenComplaints() {
        return complaintRepository.findByStatus(ComplaintStatus.OPEN).stream()
                .map(DomainMapper::toComplaintInfo)
                .toList();
    }
}
