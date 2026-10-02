package io.agenticai.rpt.contract;

import io.agenticai.rpt.error.ReportStoreException;
import io.agenticai.rpt.error.ReportStoreTexts;

/**
 * The base of every typed exception of the SVC-API in-process rejection table (ADR-RPT-012,
 * ADR-RPT-013 point 2) — thrown by the Check result port implementation and by
 * {@link ReportStore}. A {@link RuntimeException} (through {@link ReportStoreException}, the
 * module's error type) carrying a code of {@link ReportRejectionCodes} and its RULE / REQ message
 * from {@code messages.properties}; its status is the {@code {http}} segment of the code.
 *
 * <p>Every RPT write method declares {@code noRollbackFor = RptRefusalException.class}: a refusal
 * is decided before the first write, so the caller's transaction stays usable (the Check Engine
 * can still fail the Check in it). The one exception is {@link ReportNotStoredException} — a
 * database failure during the writes — which the same methods declare {@code rollbackFor}, the
 * more specific rule winning, so it rolls the caller's transaction back (REQ-RPT-009).
 */
public abstract class RptRefusalException extends ReportStoreException {

    /**
     * @param code       a constant of {@link ReportRejectionCodes}
     * @param messageKey the {@code messages.properties} key of the message — the code, or the code
     *                   with a {@code .SLUG} suffix for a code with several messages
     * @param cause      the original failure, or {@code null}
     * @param arguments  the message's placeholder values, in order of first appearance
     */
    protected RptRefusalException(String code, String messageKey, Throwable cause, Object... arguments) {
        super(code, ReportStoreTexts.english(messageKey, arguments), cause);
    }
}
