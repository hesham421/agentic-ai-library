package io.agenticai.integration.error;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The runtime failure of Host Integration (INT). It carries an error code of
 * {@link IntegrationErrorCodes} in the platform format {@code INT-{http}[-{SLUG}]}; its HTTP
 * status is the {@code {http}} segment of that code (ADR-INT-003, ADR-INT-017). The message is the
 * catalog's English text with the given arguments filled in.
 *
 * <p>Refusals of the Check Engine, Document Access and the Report Store are NOT wrapped in this
 * exception: they travel as the owners' {@code .contract} exceptions and are passed through with
 * their own code, status and message (REQ-INT-006). API misuse inside INT (a {@code null} where a
 * value is required) is signalled with {@link Objects#requireNonNull} or
 * {@link IllegalArgumentException}, never with this exception.
 */
public class IntegrationException extends RuntimeException {

    private static final Pattern CODE_FORMAT =
            Pattern.compile("^INT-(\\d{3})(?:-[A-Z0-9]+(?:-[A-Z0-9]+)*)?$");

    private final String code;
    private final int status;

    /**
     * @param code      a constant of {@link IntegrationErrorCodes}
     * @param arguments the values of the catalog message's placeholders, in order of appearance
     */
    public IntegrationException(String code, Object... arguments) {
        this(code, (Throwable) null, arguments);
    }

    /**
     * @param code      a constant of {@link IntegrationErrorCodes}
     * @param cause     the original failure, kept as the cause
     * @param arguments the values of the catalog message's placeholders, in order of appearance
     */
    public IntegrationException(String code, Throwable cause, Object... arguments) {
        this(code, IntegrationMessages.english(code, arguments), cause);
    }

    /**
     * For subclasses that supply a message already filled in. The code must still be of the form
     * {@code INT-{http}[-{SLUG}]}, from which the status is taken.
     *
     * @param code        the code, {@code INT-{http}[-{SLUG}]}
     * @param messageText the message, already filled in
     * @param cause       the original failure, or {@code null}
     */
    protected IntegrationException(String code, String messageText, Throwable cause) {
        super(messageText, cause);
        this.code = code;
        this.status = statusOf(code);
    }

    /** The error code, {@code INT-{http}[-{SLUG}]}. */
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
                    "Error code \"" + code + "\" is not of the form INT-{http}[-{SLUG}]");
        }
        return Integer.parseInt(matcher.group(1));
    }
}
