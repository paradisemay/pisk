package ru.ifmo.se.pisk.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "registration_date", nullable = false)
    private Instant registrationDate = Instant.now();

    @Column(name = "password_hash", nullable = false)
    private byte[] passwordHash;

    @Column(name = "password_salt", nullable = false)
    private byte[] passwordSalt;

    @Column(name = "password_alg", nullable = false)
    private String passwordAlg = "bcrypt";

    @Column(name = "avatar_path")
    private String avatarPath;

    @Column(nullable = false)
    private short role = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role", insertable = false, updatable = false)
    private Role roleEntity;
}
