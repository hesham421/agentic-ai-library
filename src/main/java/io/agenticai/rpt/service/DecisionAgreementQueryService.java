package io.agenticai.rpt.service;

import io.agenticai.rpt.contract.AgreementRow;
import io.agenticai.rpt.contract.ServiceCodeMissingException;
import io.agenticai.rpt.error.ReportStoreException;
import io.agenticai.rpt.repository.CheckRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * The decision agreement of a service — API-RPT-003 / CON-RPT-005 (REQ-RPT-040, REQ-RPT-041); the
 * same method serves the HTTP controller and {@code ReportStore}. READ_ONLY; writes nothing.
 * Module-internal.
 */
@Service
public class DecisionAgreementQueryService {

    private static final Logger log = LoggerFactory.getLogger(DecisionAgreementQueryService.class);

    private final CheckRunRepository checkRuns;

    public DecisionAgreementQueryService(CheckRunRepository checkRuns) {
        this.checkRuns = Objects.requireNonNull(checkRuns, "checkRuns");
    }

    /**
     * API-RPT-003 orchestration: RULE-RPT-015 (a service code present and not blank, else
     * RPT-400-SERVICE-CODE-MISSING) → QR-RPT-007 with the service code bound → the rows, ordered by
     * version number descending, Overall Status, decision; only decided Checks; empty when none.
     */
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true, noRollbackFor = ReportStoreException.class)
    public List<AgreementRow> read(String serviceCode) {
        if (serviceCode == null || serviceCode.isBlank()) {
            throw new ServiceCodeMissingException();
        }
        List<AgreementRow> rows = checkRuns.findDecisionAgreement(serviceCode).stream()
                .map(row -> new AgreementRow(row.versionNumber(), row.overallStatus().storedValue(),
                        row.employeeDecision().storedValue(), row.count()))
                .toList();
        log.debug("RPT read decision agreement rows={}", rows.size());
        return rows;
    }
}
