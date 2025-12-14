package ru.ifmo.se.pisk.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class Sha384PasswordEncoderTest {

    private static final String PEPPER = "test-pepper";

    @Test
    void encodesPasswordWithSaltAndPepper() throws Exception {
        Sha384PasswordEncoder encoder = new Sha384PasswordEncoder(PEPPER);
        String rawPassword = "qwerty";
        String salt = "abcd1234";

        String encoded = encoder.encodeWithSalt(rawPassword, salt);

        MessageDigest digest = MessageDigest.getInstance("SHA-384");
        byte[] expected = digest.digest((rawPassword + salt + PEPPER).getBytes(StandardCharsets.UTF_8));

        assertThat(encoded).isEqualTo(HexFormat.of().formatHex(expected));
    }

    @Test
    void doesNotMatchLegacyBcryptHashes() {
        Sha384PasswordEncoder encoder = new Sha384PasswordEncoder(PEPPER);

        assertThat(encoder.matches("qwerty", "$2a$10$123456789012345678901uPEiFEueQM4iAtke8o9hrIBsZotMytYO"))
                .isFalse();
    }
}
