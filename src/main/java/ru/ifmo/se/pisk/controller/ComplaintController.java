package ru.ifmo.se.pisk.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ifmo.se.pisk.controller.dto.ComplaintResponse;
import ru.ifmo.se.pisk.controller.dto.CreateComplaintRequest;
import ru.ifmo.se.pisk.controller.dto.ResolveComplaintRequest;
import ru.ifmo.se.pisk.controller.mapper.ApiMapper;
import ru.ifmo.se.pisk.service.ComplaintService;
import ru.ifmo.se.pisk.service.dto.ComplaintDecisionCommand;

@RestController
@RequestMapping("/api/v1/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @PostMapping
    public ComplaintResponse create(@Valid @RequestBody CreateComplaintRequest request) {
        return ApiMapper.toComplaintResponse(complaintService.createComplaint(request.trackId(), request.complainantId(), request.reason()));
    }

    @PutMapping("/{id}")
    public ComplaintResponse resolve(@PathVariable Long id, @Valid @RequestBody ResolveComplaintRequest request) {
        ComplaintDecisionCommand command = new ComplaintDecisionCommand(id, request.moderatorId(), request.status(), request.note());
        return ApiMapper.toComplaintResponse(complaintService.decideComplaint(command));
    }

    @GetMapping("/open")
    public List<ComplaintResponse> listOpen() {
        return complaintService.listOpenComplaints().stream().map(ApiMapper::toComplaintResponse).toList();
    }
}
