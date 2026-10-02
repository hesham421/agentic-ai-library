package io.agenticai.reg.error;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The runtime failure of the Service Registry (REG). It carries a registered error code of the
 * module's {@code error-catalog} ({@link ServiceRegistryErrorCodes}); its HTTP status is the
 * {@code {http}} segment of that code and its message is the catalog's English text with the
 * given arguments filled in. Traces: ADR-REG-011 (the REG v1 error catalog — {@code REG-404-SERVICE-NOT-FOUND}
 * of API-REG-002, {@code REG-500} of API-REG-001 … API-REG-003).
 *
 * <p>API misuse inside the service (a {@code null} where a value is required) is not a
 * {@code ServiceRegistryException}: it is signalled with {@link Objects#requireNonNull} or
 * {@link IllegalArgumentException}.
 */
public class ServiceRegistryException extends RuntimeException {

    private static final Pattern CODE_FORMAT =
            Pattern.compile("^REG-(\\d{3})(?:-[A-Z0-9]+(?:-[A-Z0-9]+)*)?$");

    private final String code;
    private final int status;

    /**
     * @param code      a constant of {@link ServiceRegistryErrorCodes}
     * @param arguments the values of the catalog message's placeholders, in order of appearance
     */
    public ServiceRegistryException(String code, Object... arguments) {
        this(code, (Throwable) null, arguments);
    }

    /**
     * @param code      a constant of {@link ServiceRegistryErrorCodes}
     * @param cause     the original failure, kept as the cause
     * @param arguments the values of the catalog message's placeholders, in order of appearance
     */
    public ServiceRegistryException(String code, Throwable cause, Object... arguments) {
        super(ServiceRegistryMessages.english(code, arguments), cause);
        this.code = code;
        this.status = statusOf(code);
    }

    /** The error code, {@code {MOD}-{http}[-{SLUG}]}. */
    public String code() {
        return code;
    }

    /** The HTTP status, the {@code {http}} segment of {@link #code()}. */
    public int status() {
        return status;
    }

    private static int statusOf(String code) {
        Objects.requireNonNull(code, "code");
        Matcher matcher = CODE_FORMAT.matcher(code);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Error code \"" + code + "\" is not of the form REG-{http}[-{SLUG}]");
        }
        return Integer.parseInt(matcher.group(1));
    }
}
