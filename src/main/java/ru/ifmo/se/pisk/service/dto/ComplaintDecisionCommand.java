package ru.ifmo.se.pisk.service.dto;

import ru.ifmo.se.pisk.domain.ComplaintStatus;

public record ComplaintDecisionCommand(
        Long complaintId,
        Long moderatorId,
        ComplaintStatus targetStatus,
        String moderatorNote
) {
}
