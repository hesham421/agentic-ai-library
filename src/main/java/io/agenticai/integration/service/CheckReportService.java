package io.agenticai.integration.service;

import io.agenticai.integration.dto.CheckReportResponse;
import io.agenticai.integration.dto.ChecksOfRequestResponse;
import io.agenticai.integration.port.CheckRecordPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * API-INT-005 and API-INT-006 — the Report Store's reads, relayed (CON-INT-005, CON-INT-006;
 * ADR-INT-020). Each read relays one owner operation unchanged and keeps nothing (REQ-INT-059);
 * INT derives nothing (ADR-INT-011 (2)). The Report Store's refusals
 * ({@code RPT-404-CHECK-NOT-FOUND}, {@code RPT-400-REQUEST-KEYS-MISSING}) pass through.
 *
 * <p>No {@code @Transactional}: INT owns no table; the Report Store's reads run in its own
 * read-only transactions.
 */
@Service("intCheckReportService")
public class CheckReportService {

    private static final Logger log = LoggerFactory.getLogger(CheckReportService.class);

    private final CheckRecordPort checkRecords;

    public CheckReportService(CheckRecordPort checkRecords) {
        this.checkRecords = Objects.requireNonNull(checkRecords, "checkRecords");
    }

    /** API-INT-005 — the Check and its report, as the Report Store holds it. */
    public CheckReportResponse read(Long checkId) {
        Objects.requireNonNull(checkId, "checkId");
        CheckReportResponse report = CheckReportResponse.of(checkRecords.readReport(checkId));
        log.debug("INT read check report checkId={}", checkId);
        return report;
    }

    /**
     * API-INT-006 — the Checks of a request. Both keys are passed exactly as received, absent ones
     * as {@code null}: the Report Store decides their presence (RPT-400-REQUEST-KEYS-MISSING).
     */
    public ChecksOfRequestResponse list(String serviceCode, String requestNumber) {
        ChecksOfRequestResponse list = ChecksOfRequestResponse.of(checkRecords.listOfRequest(serviceCode, requestNumber));
        log.debug("INT read checks of request returned={} total={}", list.checks().size(), list.total());
        return list;
    }
}
