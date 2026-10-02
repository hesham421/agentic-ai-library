package io.agenticai.integration.port;

import io.agenticai.integration.domain.CheckSnapshot;
import io.agenticai.rpt.contract.CheckReport;
import io.agenticai.rpt.contract.ChecksOfRequest;
import io.agenticai.rpt.contract.RecordedDecision;

/**
 * INT's port to the Report Store (PORTS; REQ-INT-009, REQ-INT-021, REQ-INT-061, REQ-INT-062). The
 * Report Store's refusals ({@code RPT-404-CHECK-NOT-FOUND}, {@code RPT-400-DECISION-INCOMPLETE},
 * {@code RPT-409-CHECK-NOT-COMPLETED}, {@code RPT-409-DECISION-ALREADY-RECORDED},
 * {@code RPT-422-APPROVAL-FLAG-ON-REJECTION}, {@code RPT-400-REQUEST-KEYS-MISSING}) propagate
 * through it unchanged.
 */
public interface CheckRecordPort {

    /** What INT needs of one Check before it acts on it. */
    CheckSnapshot read(Long checkId);

    /** The Check and its whole report, exactly as the Report Store answers it. */
    CheckReport readReport(Long checkId);

    /** The Checks of one request, {@code {total, checks}} unchanged. */
    ChecksOfRequest listOfRequest(String serviceCode, String requestNumber);

    /** Hands the Employee Decision over to be recorded; returns the recorded decision. */
    RecordedDecision handOverDecision(Long checkId, String employeeDecision, String decidedBy,
                                      boolean approvalApiExecuted);
}
