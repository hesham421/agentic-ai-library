package io.agenticai.rpt.service;

import io.agenticai.rpt.contract.AgreementRow;
import io.agenticai.rpt.contract.CheckReport;
import io.agenticai.rpt.contract.ChecksOfRequest;
import io.agenticai.rpt.contract.RecordedDecision;
import io.agenticai.rpt.contract.ReportNotStoredException;
import io.agenticai.rpt.contract.ReportStore;
import io.agenticai.rpt.contract.RptRefusalException;
import io.agenticai.rpt.error.ReportStoreException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * The implementation of {@link ReportStore}, the in-process interface Host Integration is given
 * (contract-rpt.md, CON-RPT-003 … CON-RPT-006). Orchestration only: the reads are the very service
 * methods that serve API-RPT-001 … API-RPT-003, and {@code recordDecision} is
 * {@link CheckRunCommandService#recordDecision}, in the caller's transaction (REQUIRED). RPT never
 * calls an Approval API (REQ-RPT-037) — there is no outbound HTTP client in RPT.
 */
@Service
public class ReportStoreService implements ReportStore {

    private final CheckReportQueryService checkReports;
    private final DecisionAgreementQueryService decisionAgreement;
    private final CheckRunCommandService commands;

    public ReportStoreService(CheckReportQueryService checkReports,
                              DecisionAgreementQueryService decisionAgreement,
                              CheckRunCommandService commands) {
        this.checkReports = Objects.requireNonNull(checkReports, "checkReports");
        this.decisionAgreement = Objects.requireNonNull(decisionAgreement, "decisionAgreement");
        this.commands = Objects.requireNonNull(commands, "commands");
    }

    /** CON-RPT-003 — honoured by {@link CheckReportQueryService#read}. */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true, noRollbackFor = ReportStoreException.class)
    public CheckReport readCheck(Long checkId) {
        return checkReports.read(checkId);
    }

    /** CON-RPT-004 — honoured by {@link CheckReportQueryService#listOfRequest}. */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true, noRollbackFor = ReportStoreException.class)
    public ChecksOfRequest listChecksOfRequest(String serviceCode, String requestNumber) {
        return checkReports.listOfRequest(serviceCode, requestNumber);
    }

    /** CON-RPT-005 — honoured by {@link DecisionAgreementQueryService#read}. */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true, noRollbackFor = ReportStoreException.class)
    public List<AgreementRow> readDecisionAgreement(String serviceCode) {
        return decisionAgreement.read(serviceCode);
    }

    /** CON-RPT-006 — honoured by {@link CheckRunCommandService#recordDecision}. */
    @Override
    @Transactional(propagation = Propagation.REQUIRED,
            noRollbackFor = RptRefusalException.class, rollbackFor = ReportNotStoredException.class)
    public RecordedDecision recordDecision(Long checkId, String employeeDecision, String decidedBy,
                                           Boolean approvalApiExecuted) {
        return commands.recordDecision(checkId, employeeDecision, decidedBy, approvalApiExecuted);
    }
}
