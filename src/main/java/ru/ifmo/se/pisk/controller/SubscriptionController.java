package ru.ifmo.se.pisk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.ifmo.se.pisk.controller.dto.SubscriptionResponse;
import ru.ifmo.se.pisk.controller.dto.UpdateSubscriptionStatusRequest;
import ru.ifmo.se.pisk.controller.mapper.ApiMapper;
import ru.ifmo.se.pisk.service.SubscriptionService;

@RestController
@Tag(name = "Subscriptions", description = "Подписки на артистов")
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    @Operation(summary = "Оформить подписку", description = "Создаёт подписку слушателя на артиста")
    public SubscriptionResponse subscribe(@RequestParam Long followerId, @RequestParam Long artistId) {
        return ApiMapper.toSubscriptionResponse(subscriptionService.subscribe(followerId, artistId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить статус подписки", description = "Изменяет статус подписки модератором")
    public SubscriptionResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateSubscriptionStatusRequest request) {
        return ApiMapper.toSubscriptionResponse(subscriptionService.updateStatus(id, request.status(), request.moderatorId()));
    }
}
