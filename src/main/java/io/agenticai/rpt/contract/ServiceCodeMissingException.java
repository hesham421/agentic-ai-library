package io.agenticai.rpt.contract;

import io.agenticai.rpt.error.ReportStoreErrorCodes;
import io.agenticai.rpt.error.ReportStoreException;

/**
 * RULE-RPT-015 — {@code serviceCode} absent or blank when reading the decision agreement
 * (API-RPT-003, CON-RPT-005 {@code readDecisionAgreement}); nothing is read.
 * Code {@code RPT-400-SERVICE-CODE-MISSING} with its catalog text, unchanged.
 *
 * <p>Published in {@code .contract} so that a caller of {@link ReportStore} can relay the refusal
 * unchanged without importing {@code rpt.error}. It is a {@link ReportStoreException} — RPT's own
 * advice answers it exactly as before — and deliberately not an {@link RptRefusalException}.
 */
public class ServiceCodeMissingException extends ReportStoreException {

    public ServiceCodeMissingException() {
        super(ReportStoreErrorCodes.SERVICE_CODE_MISSING);
    }
}
