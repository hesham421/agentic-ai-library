package io.agenticai.reg.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * The content hash of a service package version (DBF-REG-018, REQ-REG-021, RULE-REG-003,
 * ADR-REG-003): {@code SHA-256( UTF-8(knowledge) ‖ 0x00 ‖ UTF-8(definition) )}, lower-case hex,
 * 64 characters. The single {@code NUL} separator keeps the two files apart so a boundary shift
 * between them changes the digest; the recipe is fixed — a stored hash is compared against the
 * same recipe at every later load run. Pure.
 */
final class ContentHash {

    private static final byte[] SEPARATOR = {0};

    private ContentHash() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    static String of(String knowledgeText, String definitionText) {
        Objects.requireNonNull(knowledgeText, "knowledgeText");
        Objects.requireNonNull(definitionText, "definitionText");
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every Java platform", e);
        }
        digest.update(knowledgeText.getBytes(StandardCharsets.UTF_8));
        digest.update(SEPARATOR);
        digest.update(definitionText.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest.digest());
    }
}
