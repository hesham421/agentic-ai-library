package io.agenticai.rpt.contract;

import io.agenticai.rpt.error.ReportStoreErrorCodes;
import io.agenticai.rpt.error.ReportStoreException;

/**
 * RULE-RPT-009 — {@code serviceCode} or {@code requestNumber} absent or blank when listing the
 * Checks of a request (API-RPT-002, CON-RPT-004 {@code listChecksOfRequest}); nothing is read.
 * Code {@code RPT-400-REQUEST-KEYS-MISSING} with its catalog text, unchanged.
 *
 * <p>Published in {@code .contract} so that a caller of {@link ReportStore} (INT's API-INT-006)
 * can relay the refusal unchanged without importing {@code rpt.error}. It is a
 * {@link ReportStoreException} — RPT's own advice answers it exactly as before — and deliberately
 * not an {@link RptRefusalException}.
 */
public class RequestKeysMissingException extends ReportStoreException {

    public RequestKeysMissingException() {
        super(ReportStoreErrorCodes.REQUEST_KEYS_MISSING);
    }
}
