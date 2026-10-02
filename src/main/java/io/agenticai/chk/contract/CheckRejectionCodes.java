package io.agenticai.chk.contract;

/**
 * The in-process rejection codes of the Check Engine — the rows of the SVC-API in-process
 * rejection table (ADR-CHK-018): typed exceptions of the {@link CheckEngine} interface, each
 * carrying a code in the profile format {@code CHK-{http}[-{SLUG}]} and the SRS message. They are
 * NOT rows of the module's error catalog (that holds only the three PLATFORM-STD rows of
 * API-CHK-001, kept in {@code CheckEngineErrorCodes}): INT maps them to its own ProblemDetail.
 * Their English texts are in {@code messages.properties}, keyed by code.
 */
public final class CheckRejectionCodes {

    private CheckRejectionCodes() {
        throw new UnsupportedOperationException("Constants class, do not instantiate");
    }

    /** REQ-CHK-004 — a start value is absent or blank ({@link StartIncompleteException}). */
    public static final String START_INCOMPLETE = "CHK-400-START-INCOMPLETE";

    /** RULE-CHK-001 — the service is not available for Checks ({@link ServiceNotAvailableException}). */
    public static final String SERVICE_NOT_AVAILABLE = "CHK-422-SERVICE-NOT-AVAILABLE";

    /** REQ-CHK-006 — a connection of the current version is not activated ({@link ConnectionNotActivatedException}). */
    public static final String CONNECTION_NOT_ACTIVATED = "CHK-422-CONNECTION-NOT-ACTIVATED";

    /** REQ-CHK-058 — the result port knows no such Check ({@link CheckNotFoundException}). */
    public static final String CHECK_NOT_FOUND = "CHK-404-CHECK-NOT-FOUND";

    /** RULE-CHK-010 — the Check is not waiting for documents ({@link CheckNotAwaitingDocumentsException}). */
    public static final String CHECK_NOT_AWAITING_DOCUMENTS = "CHK-409-CHECK-NOT-AWAITING-DOCUMENTS";
}
