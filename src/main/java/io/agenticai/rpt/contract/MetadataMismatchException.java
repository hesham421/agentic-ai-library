package io.agenticai.rpt.contract;

import java.util.Objects;

/**
 * RULE-RPT-004 — the report metadata of {@code completeCheck} is missing a value or differs from
 * the stored Check run; nothing is stored (REQ-RPT-013). In-process code
 * {@value ReportRejectionCodes#METADATA_MISMATCH}, with one of the RULE's two messages.
 */
public class MetadataMismatchException extends RptRefusalException {

    private static final String DIFFERS = ReportRejectionCodes.METADATA_MISMATCH + ".DIFFERS";
    private static final String MISSING = ReportRejectionCodes.METADATA_MISMATCH + ".MISSING";

    private MetadataMismatchException(String messageKey, Object... arguments) {
        super(ReportRejectionCodes.METADATA_MISMATCH, messageKey, null, arguments);
    }

    /**
     * "The report of Check {checkId} was not stored: its metadata {field} {value} differs from the
     * Check run ({stored})."
     */
    public static MetadataMismatchException differs(Long checkId, String field, String value, String stored) {
        return new MetadataMismatchException(DIFFERS, Objects.requireNonNull(checkId, "checkId"), field, value, stored);
    }

    /** "The report of Check {checkId} was not stored: {field} is missing." */
    public static MetadataMismatchException missing(Long checkId, String field) {
        return new MetadataMismatchException(MISSING, Objects.requireNonNull(checkId, "checkId"), field);
    }
}
