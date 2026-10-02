package io.agenticai.rpt.error;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The runtime failure of the Report Store (RPT). It carries an error code in the platform format
 * {@code RPT-{http}[-{SLUG}]}; its HTTP status is the {@code {http}} segment of that code
 * (ADR-RPT-013). Two kinds of code exist, and the constructors keep them apart:
 *
 * <ul>
 *   <li>a catalog code — a constant of {@link ReportStoreErrorCodes} — whose message is the
 *       catalog's English text with the given arguments filled in (the public constructors);</li>
 *   <li>an in-process refusal code of the Check result port or of the Employee Decision (the
 *       SVC-API table), which carries its RULE / REQ message and is not an HTTP catalog row
 *       (ADR-RPT-013 point 2). A subclass of the in-process interface supplies the code and the
 *       message text itself through the
 *       {@linkplain #ReportStoreException(String, String, Throwable) protected constructor}, so
 *       the catalog and {@link ReportStoreErrorCodes} stay exactly the catalog's rows.</li>
 * </ul>
 *
 * <p>API misuse inside the service (a {@code null} where a value is required) is not a
 * {@code ReportStoreException}: it is signalled with {@link Objects#requireNonNull} or
 * {@link IllegalArgumentException}.
 */
public class ReportStoreException extends RuntimeException {

    private static final Pattern CODE_FORMAT =
            Pattern.compile("^RPT-(\\d{3})(?:-[A-Z0-9]+(?:-[A-Z0-9]+)*)?$");

    private final String code;
    private final int status;

    /**
     * @param code      a constant of {@link ReportStoreErrorCodes}
     * @param arguments the values of the catalog message's placeholders, in order of appearance
     */
    public ReportStoreException(String code, Object... arguments) {
        this(code, (Throwable) null, arguments);
    }

    /**
     * @param code      a constant of {@link ReportStoreErrorCodes}
     * @param cause     the original failure, kept as the cause
     * @param arguments the values of the catalog message's placeholders, in order of appearance
     */
    public ReportStoreException(String code, Throwable cause, Object... arguments) {
        this(code, ReportStoreMessages.english(code, arguments), cause);
    }

    /**
     * For subclasses that carry a code outside the HTTP catalog — the in-process rejections of
     * ADR-RPT-013, whose message is the SRS text rather than a catalog text. The code must still
     * be of the form {@code RPT-{http}[-{SLUG}]}, from which the status is taken.
     *
     * @param code        the code, {@code RPT-{http}[-{SLUG}]}
     * @param messageText the message, already filled in
     * @param cause       the original failure, or {@code null}
     */
    protected ReportStoreException(String code, String messageText, Throwable cause) {
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
                    "Error code \"" + code + "\" is not of the form RPT-{http}[-{SLUG}]");
        }
        return Integer.parseInt(matcher.group(1));
    }
}
