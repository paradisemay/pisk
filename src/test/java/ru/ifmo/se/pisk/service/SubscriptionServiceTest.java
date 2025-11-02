package ru.ifmo.se.pisk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ifmo.se.pisk.domain.SubscriptionStatus;
import ru.ifmo.se.pisk.repository.SubscriptionRepository;
import ru.ifmo.se.pisk.repository.entity.SubscriptionEntity;
import ru.ifmo.se.pisk.repository.entity.UserEntity;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private UserService userService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private UserEntity follower;
    private UserEntity artist;

    @BeforeEach
    void setUp() {
        follower = new UserEntity();
        follower.setId(1L);
        artist = new UserEntity();
        artist.setId(2L);
        when(userService.requireUser(1L)).thenReturn(follower);
        when(userService.requireUser(2L)).thenReturn(artist);
    }

    @Test
    void subscribeCreatesNewRecord() {
        SubscriptionEntity saved = new SubscriptionEntity();
        saved.setId(10L);
        saved.setFollower(follower);
        saved.setArtist(artist);
        saved.setStatus(SubscriptionStatus.ACTIVE);
        when(subscriptionRepository.findByFollowerAndArtist(follower, artist)).thenReturn(java.util.Optional.empty());
        when(subscriptionRepository.save(any(SubscriptionEntity.class))).thenReturn(saved);

        var response = subscriptionService.subscribe(1L, 2L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(auditService).record(any(), anyString(), anyString(), anyLong(), anyMap());
    }
}
