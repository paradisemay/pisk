package ru.ifmo.se.pisk.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Objects;
import org.springframework.security.crypto.password.PasswordEncoder;

public class Sha384PasswordEncoder implements PasswordEncoder {

    private static final int DEFAULT_SALT_BYTES = 16;

    private final SecureRandom secureRandom = new SecureRandom();
    private final String pepper;

    public Sha384PasswordEncoder(String pepper) {
        this.pepper = Objects.requireNonNull(pepper, "pepper");
    }

    public String generateSalt() {
        byte[] salt = new byte[DEFAULT_SALT_BYTES];
        secureRandom.nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    public String encodeWithSalt(CharSequence rawPassword, String salt) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Пароль не может быть null");
        }
        if (salt == null) {
            throw new IllegalArgumentException("Соль не может быть null");
        }
        MessageDigest digest = newDigest();
        byte[] hash = digest.digest((rawPassword.toString() + salt + pepper).getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    public boolean matchesWithSalt(CharSequence rawPassword, String salt, String encodedPassword) {
        return encodeWithSalt(rawPassword, salt).equals(encodedPassword);
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return encodeWithSalt(rawPassword, "");
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return encode(rawPassword).equals(encodedPassword);
    }

    private MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-384");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-384 не поддерживается", ex);
        }
    }
}
