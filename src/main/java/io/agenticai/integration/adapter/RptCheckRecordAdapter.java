package io.agenticai.integration.adapter;

import io.agenticai.integration.domain.CheckSnapshot;
import io.agenticai.integration.port.CheckRecordPort;
import io.agenticai.rpt.contract.CheckReport;
import io.agenticai.rpt.contract.ChecksOfRequest;
import io.agenticai.rpt.contract.RecordedDecision;
import io.agenticai.rpt.contract.ReportStore;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * {@link CheckRecordPort} over the Report Store's published in-process interface
 * {@link ReportStore} (PORTS; REQ-INT-009, REQ-INT-021, REQ-INT-061, REQ-INT-062).
 *
 * <ul>
 *   <li>{@code read} calls {@code readCheck} and keeps the checkId, status, service code, version
 *       number, request number and decision code as a {@link CheckSnapshot} — the owners' codes,
 *       unchanged (ADR-INT-013).</li>
 *   <li>{@code readReport} calls the same {@code readCheck} and returns the report unchanged — the
 *       Report Store's own {@link CheckReport}, not a copy (INT derives nothing, ADR-INT-011).</li>
 *   <li>{@code listOfRequest} and {@code handOverDecision} relay {@code listChecksOfRequest} and
 *       {@code recordDecision} unchanged.</li>
 * </ul>
 * The Report Store's refusals are not caught: they reach {@code IntegrationProblemAdvice} unchanged
 * (REQ-INT-006). Stateless (REQ-INT-059).
 */
@Component("intRptCheckRecordAdapter")
public class RptCheckRecordAdapter implements CheckRecordPort {

    private final ReportStore reportStore;

    public RptCheckRecordAdapter(ReportStore reportStore) {
        this.reportStore = Objects.requireNonNull(reportStore, "reportStore");
    }

    @Override
    public CheckSnapshot read(Long checkId) {
        CheckReport report = readReport(checkId);
        return new CheckSnapshot(
                report.checkId(),
                report.status(),
                report.serviceCode(),
                report.versionNumber(),
                report.requestNumber(),
                report.decision() == null ? null : report.decision().employeeDecision());
    }

    @Override
    public CheckReport readReport(Long checkId) {
        return reportStore.readCheck(Objects.requireNonNull(checkId, "checkId"));
    }

    @Override
    public ChecksOfRequest listOfRequest(String serviceCode, String requestNumber) {
        return reportStore.listChecksOfRequest(serviceCode, requestNumber);
    }

    @Override
    public RecordedDecision handOverDecision(Long checkId, String employeeDecision, String decidedBy,
                                             boolean approvalApiExecuted) {
        return reportStore.recordDecision(Objects.requireNonNull(checkId, "checkId"),
                employeeDecision, decidedBy, approvalApiExecuted);
    }
}
