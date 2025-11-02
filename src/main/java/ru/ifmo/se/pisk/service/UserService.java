package ru.ifmo.se.pisk.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ifmo.se.pisk.domain.AuditAction;
import ru.ifmo.se.pisk.domain.UserProfile;
import ru.ifmo.se.pisk.domain.UserRole;
import ru.ifmo.se.pisk.domain.UserStatus;
import ru.ifmo.se.pisk.domain.entity.UserEntity;
import ru.ifmo.se.pisk.domain.mapper.DomainMapper;
import ru.ifmo.se.pisk.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuditService auditService) {
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
        UserEntity entity = new UserEntity();
        entity.setUsername(username);
        entity.setEmail(email);
        entity.setPasswordHash(passwordEncoder.encode(rawPassword));
        entity.setRole(role);
        entity.setStatus(UserStatus.PENDING);
        entity.setCreatedAt(Instant.now());
        UserEntity saved = userRepository.save(entity);
        auditService.record(AuditAction.USER_REGISTERED, "User", saved.getId().toString(), saved.getId(), Map.of());
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
}
