package ru.ifmo.se.pisk.service;

import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ifmo.se.pisk.domain.AuditAction;
import ru.ifmo.se.pisk.domain.SubscriptionInfo;
import ru.ifmo.se.pisk.domain.SubscriptionStatus;
import ru.ifmo.se.pisk.domain.entity.SubscriptionEntity;
import ru.ifmo.se.pisk.domain.entity.UserEntity;
import ru.ifmo.se.pisk.domain.mapper.DomainMapper;
import ru.ifmo.se.pisk.repository.SubscriptionRepository;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserService userService;
    private final AuditService auditService;

    public SubscriptionService(SubscriptionRepository subscriptionRepository, UserService userService, AuditService auditService) {
        this.subscriptionRepository = subscriptionRepository;
        this.userService = userService;
        this.auditService = auditService;
    }

    @Transactional
    public SubscriptionInfo subscribe(Long followerId, Long artistId) {
        UserEntity follower = userService.requireUser(followerId);
        UserEntity artist = userService.requireUser(artistId);
        if (follower.equals(artist)) {
            throw new IllegalArgumentException("Нельзя подписаться на самого себя");
        }
        SubscriptionEntity entity = subscriptionRepository.findByFollowerAndArtist(follower, artist)
                .orElseGet(() -> {
                    SubscriptionEntity subscription = new SubscriptionEntity();
                    subscription.setFollower(follower);
                    subscription.setArtist(artist);
                    return subscription;
                });
        entity.setStatus(SubscriptionStatus.ACTIVE);
        SubscriptionEntity saved = subscriptionRepository.save(entity);
        auditService.record(AuditAction.SUBSCRIPTION_UPDATED, "Subscription", saved.getId().toString(), follower,
                Map.of(
                        "status", saved.getStatus().name(),
                        "follower", saved.getFollower().getId().toString(),
                        "artist", saved.getArtist().getId().toString()
                ));
        return DomainMapper.toSubscriptionInfo(saved);
    }

    @Transactional
    public SubscriptionInfo updateStatus(Long subscriptionId, SubscriptionStatus status, Long moderatorId) {
        SubscriptionEntity entity = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Подписка не найдена"));
        UserEntity moderator = userService.requireUser(moderatorId);
        entity.setStatus(status);
        auditService.record(AuditAction.SUBSCRIPTION_UPDATED, "Subscription", subscriptionId.toString(), moderator,
                Map.of(
                        "status", status.name(),
                        "follower", entity.getFollower().getId().toString(),
                        "artist", entity.getArtist().getId().toString()
                ));
        return DomainMapper.toSubscriptionInfo(entity);
    }
}
