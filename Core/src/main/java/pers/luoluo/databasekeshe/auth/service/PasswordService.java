package pers.luoluo.databasekeshe.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

    private static final String NOOP_PREFIX = "{noop}";
    private static final String PBKDF2_PREFIX = "{pbkdf2}";
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final int ITERATIONS = 120_000;

    private final SecureRandom secureRandom = new SecureRandom();

    public String hash(String rawPassword) {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] hash = pbkdf2(rawPassword.toCharArray(), salt, ITERATIONS);

        return PBKDF2_PREFIX
                + ITERATIONS
                + ":"
                + Base64.getEncoder().withoutPadding().encodeToString(salt)
                + ":"
                + Base64.getEncoder().withoutPadding().encodeToString(hash);
    }

    public boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }

        if (storedHash.startsWith(NOOP_PREFIX)) {
            return constantTimeEquals(rawPassword, storedHash.substring(NOOP_PREFIX.length()));
        }

        if (storedHash.startsWith(PBKDF2_PREFIX)) {
            return matchesPbkdf2(rawPassword, storedHash);
        }

        return false;
    }

    private boolean matchesPbkdf2(String rawPassword, String storedHash) {
        String encoded = storedHash.substring(PBKDF2_PREFIX.length());
        String[] parts = encoded.split(":");

        if (parts.length != 3) {
            return false;
        }

        try {
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[2]);
            byte[] actualHash = pbkdf2(rawPassword.toCharArray(), salt, iterations);

            return MessageDigest.isEqual(actualHash, expectedHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, HASH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
            throw new IllegalStateException("Cannot hash password", exception);
        }
    }

    private boolean constantTimeEquals(String rawPassword, String expectedPassword) {
        byte[] rawPasswordBytes = rawPassword.getBytes(StandardCharsets.UTF_8);
        byte[] expectedPasswordBytes = expectedPassword.getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(rawPasswordBytes, expectedPasswordBytes);
    }
}
