package ru.ifmo.se.pisk.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ifmo.se.pisk.domain.AuditAction;
import ru.ifmo.se.pisk.domain.UserProfile;
import ru.ifmo.se.pisk.domain.UserRole;
import ru.ifmo.se.pisk.domain.UserStatus;
import ru.ifmo.se.pisk.domain.entity.UserEntity;
import ru.ifmo.se.pisk.domain.mapper.DomainMapper;
import ru.ifmo.se.pisk.repository.UserRepository;
import ru.ifmo.se.pisk.security.Sha384PasswordEncoder;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final Sha384PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, Sha384PasswordEncoder passwordEncoder, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public UserProfile registerUser(String username, String email, String rawPassword, UserRole role) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new IllegalArgumentException("Имя пользователя уже занято");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email уже используется");
        }
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new IllegalArgumentException("Пароль должен содержать не менее 8 символов");
        }
        UserEntity entity = new UserEntity();
        entity.setUsername(username);
        entity.setEmail(email);
        String salt = passwordEncoder.generateSalt();
        entity.setPasswordSalt(salt);
        entity.setPasswordHash(passwordEncoder.encodeWithSalt(rawPassword, salt));
        entity.setRole(role);
        entity.setStatus(UserStatus.PENDING);
        entity.setCreatedAt(Instant.now());
        UserEntity saved = userRepository.save(entity);
        auditService.record(AuditAction.USER_REGISTERED, "User", saved.getId().toString(), saved, Map.of());
        return DomainMapper.toUserProfile(saved);
    }

    @Transactional(readOnly = true)
    public UserEntity requireUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
    }

    @Transactional
    public void updateStatus(Long userId, UserStatus status) {
        UserEntity entity = requireUser(userId);
        entity.setStatus(status);
        entity.setUpdatedAt(Instant.now());
    }

    @Transactional(readOnly = true)
    public Optional<UserProfile> findProfile(Long id) {
        return userRepository.findById(id).map(DomainMapper::toUserProfile);
    }

    @Transactional
    public void updatePassword(Long userId, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Пароль должен содержать не менее 8 символов");
        }
        UserEntity entity = requireUser(userId);
        String newSalt = passwordEncoder.generateSalt();
        entity.setPasswordSalt(newSalt);
        entity.setPasswordHash(passwordEncoder.encodeWithSalt(newPassword, newSalt));
        entity.setUpdatedAt(Instant.now());
    }
}
