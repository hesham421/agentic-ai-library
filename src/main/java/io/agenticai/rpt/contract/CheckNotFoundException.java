package io.agenticai.rpt.contract;

import java.util.Objects;

/**
 * REQ-RPT-007, REQ-RPT-025, REQ-RPT-038 — no Check Run exists for the identifier: never created or purged. In-process code {@value ReportRejectionCodes#CHECK_NOT_FOUND}.
 */
public class CheckNotFoundException extends RptRefusalException {

    private final Long checkId;

    /** @param checkId the Check identifier */
    public CheckNotFoundException(Long checkId) {
        super(ReportRejectionCodes.CHECK_NOT_FOUND, ReportRejectionCodes.CHECK_NOT_FOUND, null,
                Objects.requireNonNull(checkId, "checkId"));
        this.checkId = checkId;
    }

    public Long checkId() {
        return checkId;
    }
}
