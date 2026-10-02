package io.agenticai.doc.error;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The runtime failure of Document Access (DOC). It carries an error code in the platform format
 * {@code DOC-{http}[-{SLUG}]}; its HTTP status is the {@code {http}} segment of that code
 * (ADR-DOC-012). Two kinds of code exist, and the constructors keep them apart:
 *
 * <ul>
 *   <li>a catalog code — a constant of {@link DocumentAccessErrorCodes} — whose message is the
 *       catalog's English text with the given arguments filled in (the public constructors);</li>
 *   <li>an in-process rejection code of the handover and fetch operations, which carries the
 *       SRS rule message and is not an HTTP catalog row (ADR-DOC-012). A subclass of the
 *       in-process interface supplies the code and the message text itself through the
 *       {@linkplain #DocumentAccessException(String, String, Throwable) protected constructor},
 *       so the catalog and {@link DocumentAccessErrorCodes} stay exactly the catalog's rows.</li>
 * </ul>
 *
 * <p>API misuse inside the service (a {@code null} where a value is required) is not a
 * {@code DocumentAccessException}: it is signalled with {@link Objects#requireNonNull} or
 * {@link IllegalArgumentException}.
 */
public class DocumentAccessException extends RuntimeException {

    private static final Pattern CODE_FORMAT =
            Pattern.compile("^DOC-(\\d{3})(?:-[A-Z0-9]+(?:-[A-Z0-9]+)*)?$");

    private final String code;
    private final int status;

    /**
     * @param code      a constant of {@link DocumentAccessErrorCodes}
     * @param arguments the values of the catalog message's placeholders, in order of appearance
     */
    public DocumentAccessException(String code, Object... arguments) {
        this(code, (Throwable) null, arguments);
    }

    /**
     * @param code      a constant of {@link DocumentAccessErrorCodes}
     * @param cause     the original failure, kept as the cause
     * @param arguments the values of the catalog message's placeholders, in order of appearance
     */
    public DocumentAccessException(String code, Throwable cause, Object... arguments) {
        this(code, DocumentAccessMessages.english(code, arguments), cause);
    }

    /**
     * For subclasses that carry a code outside the HTTP catalog — the in-process rejections of
     * ADR-DOC-012, whose message is the SRS rule text rather than a catalog text. The code must
     * still be of the form {@code DOC-{http}[-{SLUG}]}, from which the status is taken.
     *
     * @param code        the code, {@code DOC-{http}[-{SLUG}]}
     * @param messageText the message, already filled in
     * @param cause       the original failure, or {@code null}
     */
    protected DocumentAccessException(String code, String messageText, Throwable cause) {
        super(messageText, cause);
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
                    "Error code \"" + code + "\" is not of the form DOC-{http}[-{SLUG}]");
        }
        return Integer.parseInt(matcher.group(1));
    }
}
