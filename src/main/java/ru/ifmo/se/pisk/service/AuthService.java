package ru.ifmo.se.pisk.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ifmo.se.pisk.domain.Role;
import ru.ifmo.se.pisk.domain.RoleType;
import ru.ifmo.se.pisk.domain.User;
import ru.ifmo.se.pisk.repository.RoleRepository;
import ru.ifmo.se.pisk.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(String username, String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        byte[] salt = new byte[0];
        String hash = passwordEncoder.encode(password);
        Role defaultRole = roleRepository.findByRole(RoleType.user)
                .orElseThrow(() -> new IllegalStateException("Default role not configured"));
        User user = new User();
        user.setUsername(username);
        user.setPasswordSalt(salt);
        user.setPasswordHash(hash.getBytes());
        user.setPasswordAlg("bcrypt");
        user.setRole(defaultRole.getId());
        return userRepository.save(user);
    }
}
