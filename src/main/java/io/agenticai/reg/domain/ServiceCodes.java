package io.agenticai.reg.domain;

import java.util.Locale;
import java.util.Objects;

/**
 * The canonical form of a service code (ADR-REG-017, REQ-REG-064): trimmed and lower-cased in
 * {@link Locale#ROOT}. It is applied to every code a package declares and to every code a read
 * or an in-process call receives, before any comparison or query; only canonical codes are stored.
 *
 * <p>Whether a canonical code is <em>valid</em> (RULE-REG-022: {@link #CANONICAL_PATTERN},
 * at most {@link #MAX_LENGTH} characters) is decided by the load run, not here.
 */
public final class ServiceCodes {

    /** The shape of a valid canonical service code (RULE-REG-022, CHK_REG_SVC_PKG_SERVICE_CODE). */
    public static final String CANONICAL_PATTERN = "^[a-z0-9]+(-[a-z0-9]+)*$";

    /** The maximum length of a service code (SERVICE_CODE VARCHAR2(100 CHAR)). */
    public static final int MAX_LENGTH = 100;

    private ServiceCodes() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    /**
     * The canonical form of {@code code}: trimmed, lower case ({@link Locale#ROOT}).
     *
     * @throws NullPointerException when {@code code} is {@code null} — a code is always given
     */
    public static String canonical(String code) {
        Objects.requireNonNull(code, "code");
        return code.trim().toLowerCase(Locale.ROOT);
    }
}
