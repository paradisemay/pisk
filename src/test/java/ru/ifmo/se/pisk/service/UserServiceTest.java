package ru.ifmo.se.pisk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.ifmo.se.pisk.domain.AuditAction;
import ru.ifmo.se.pisk.domain.UserRole;
import ru.ifmo.se.pisk.domain.UserStatus;
import ru.ifmo.se.pisk.domain.entity.UserEntity;
import ru.ifmo.se.pisk.repository.UserRepository;
import ru.ifmo.se.pisk.security.Sha384PasswordEncoder;

class UserServiceTest {

    private UserRepository userRepository;
    private AuditService auditService;
    private TestPasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        auditService = mock(AuditService.class);
        passwordEncoder = new TestPasswordEncoder("pepper", "fixedSalt");
        userService = new UserService(userRepository, passwordEncoder, auditService);
    }

    @Test
    void registersUserWithSaltedHash() {
        when(userRepository.existsByUsernameIgnoreCase("user"))
                .thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("email@example.com"))
                .thenReturn(false);
        when(userRepository.save(any())).thenAnswer(invocation -> {
            UserEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        userService.registerUser("user", "email@example.com", "secret123", UserRole.USER);

        ArgumentCaptor<UserEntity> entityCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(entityCaptor.capture());
        UserEntity saved = entityCaptor.getValue();

        assertThat(saved.getPasswordSalt()).isEqualTo("fixedSalt");
        assertThat(saved.getPasswordHash()).isEqualTo(passwordEncoder.encodeWithSalt("secret123", "fixedSalt"));
        assertThat(saved.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(saved.getCreatedAt()).isBeforeOrEqualTo(Instant.now());

        verify(auditService).record(AuditAction.USER_REGISTERED, "User", "1", saved, Map.of());
    }

    @Test
    void rejectsShortPassword() {
        assertThatThrownBy(() -> userService.registerUser("user", "email@example.com", "123", UserRole.USER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не менее 8");

        verifyNoInteractions(auditService);
    }

    private static final class TestPasswordEncoder extends Sha384PasswordEncoder {

        private final String salt;

        private TestPasswordEncoder(String pepper, String salt) {
            super(pepper);
            this.salt = salt;
        }

        @Override
        public String generateSalt() {
            return salt;
        }
    }
}
