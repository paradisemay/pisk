package ru.ifmo.se.pisk.controller;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.ifmo.se.pisk.controller.dto.AuditResponse;
import ru.ifmo.se.pisk.controller.mapper.ApiMapper;
import ru.ifmo.se.pisk.service.AuditService;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public List<AuditResponse> lastEvents() {
        return auditService.findLast(50).stream().map(ApiMapper::toAuditResponse).toList();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        SseEmitter emitter = new SseEmitter(Duration.ofMinutes(1).toMillis());
        new Thread(() -> {
            try {
                for (var record : auditService.findLast(100)) {
                    emitter.send(ApiMapper.toAuditResponse(record));
                }
                emitter.complete();
            } catch (IOException ex) {
                emitter.completeWithError(ex);
            }
        }).start();
        return emitter;
    }
}
