package io.agenticai.rpt.contract;

import java.util.Objects;

/**
 * RULE-RPT-008 — a document outcome of {@code completeCheck} carries an unreadable reason not
 * exactly when UNREADABLE, or has no document type; nothing is stored (REQ-RPT-017). In-process
 * code {@value ReportRejectionCodes#DOCUMENT_REASON_MISMATCH}, with one of the RULE's two messages.
 *
 * <p>The RULE also requires a document type on every outcome but states no message for it; that
 * case carries the same code with the plan's generic text "The report of Check {checkId} was not
 * stored." — recorded as an {@code api_doc_gaps} row, no text invented.
 */
public class DocumentReasonMismatchException extends RptRefusalException {

    private static final String REASON_MISSING = ReportRejectionCodes.DOCUMENT_REASON_MISMATCH + ".REASON-MISSING";
    private static final String REASON_NOT_ALLOWED = ReportRejectionCodes.DOCUMENT_REASON_MISMATCH + ".REASON-NOT-ALLOWED";

    private DocumentReasonMismatchException(String messageKey, Object... arguments) {
        super(ReportRejectionCodes.DOCUMENT_REASON_MISMATCH, messageKey, null, arguments);
    }

    /** "The report of Check {checkId} was not stored: document {position} is UNREADABLE without a reason." */
    public static DocumentReasonMismatchException reasonMissing(Long checkId, String position) {
        return new DocumentReasonMismatchException(REASON_MISSING, Objects.requireNonNull(checkId, "checkId"), position);
    }

    /**
     * "The report of Check {checkId} was not stored: document {position} is {readStatus} and cannot
     * carry a reason."
     */
    public static DocumentReasonMismatchException reasonNotAllowed(Long checkId, String position, String readStatus) {
        return new DocumentReasonMismatchException(REASON_NOT_ALLOWED, Objects.requireNonNull(checkId, "checkId"),
                position, readStatus);
    }

    /** A document outcome without a document type — the plan states no message of its own (gap). */
    public static DocumentReasonMismatchException documentTypeMissing(Long checkId) {
        return new DocumentReasonMismatchException(ReportRejectionCodes.REPORT_NOT_STORED,
                Objects.requireNonNull(checkId, "checkId"));
    }
}
