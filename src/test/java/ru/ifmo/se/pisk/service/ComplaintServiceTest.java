package ru.ifmo.se.pisk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ifmo.se.pisk.domain.ComplaintStatus;
import ru.ifmo.se.pisk.domain.entity.ComplaintEntity;
import ru.ifmo.se.pisk.domain.entity.TrackEntity;
import ru.ifmo.se.pisk.domain.entity.UserEntity;
import ru.ifmo.se.pisk.repository.ComplaintRepository;
import ru.ifmo.se.pisk.service.dto.ComplaintDecisionCommand;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;
    @Mock
    private TrackService trackService;
    @Mock
    private UserService userService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private ComplaintService complaintService;

    @Test
    void decideComplaintArchivesTrack() {
        ComplaintEntity entity = new ComplaintEntity();
        entity.setId(1L);
        TrackEntity track = new TrackEntity();
        track.setId(10L);
        entity.setTrack(track);
        entity.setStatus(ComplaintStatus.OPEN);
        UserEntity complainant = new UserEntity();
        complainant.setId(3L);
        entity.setComplainant(complainant);
        when(complaintRepository.findById(1L)).thenReturn(java.util.Optional.of(entity));
        UserEntity moderator = new UserEntity();
        moderator.setId(5L);
        when(userService.requireUser(5L)).thenReturn(moderator);

        ComplaintDecisionCommand command = new ComplaintDecisionCommand(1L, 5L, ComplaintStatus.RESOLVED, "ok");
        var response = complaintService.decideComplaint(command);

        assertThat(response.status()).isEqualTo(ComplaintStatus.RESOLVED);
        verify(trackService).changeStatus(10L, ru.ifmo.se.pisk.domain.TrackStatus.ARCHIVED, 5L);
        verify(auditService).record(eq(ru.ifmo.se.pisk.domain.AuditAction.COMPLAINT_RESOLVED), eq("Complaint"), eq("1"), eq(moderator), anyMap());
    }
}
