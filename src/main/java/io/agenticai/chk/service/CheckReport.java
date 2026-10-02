package io.agenticai.chk.service;

import io.agenticai.chk.contract.ReportDocumentOutcome;
import io.agenticai.chk.contract.ReportFinding;
import io.agenticai.chk.contract.ReportMetadata;
import io.agenticai.chk.contract.ReportUnreadQuery;
import io.agenticai.chk.domain.OverallStatus;
import io.agenticai.chk.domain.VerifiedFinding;
import io.agenticai.chk.port.DocumentOutcome;
import io.agenticai.chk.port.UnreadQuery;

import java.util.List;
import java.util.Objects;

/**
 * The completed report of a Check as the pipeline hands it to the ending (REQ-CHK-044,
 * REQ-CHK-045), and its one mapping to the result port's values — where document content is
 * dropped (REQ-CHK-047). Module-internal; part of the Check's working data until the ending.
 *
 * @param overallStatus    the decided Overall Status
 * @param findings         the model's verified findings plus the required-document findings
 * @param documentOutcomes the document outcomes (content still attached; dropped on mapping)
 * @param unreadQueries    the service queries not read
 * @param metadata         the report metadata
 */
record CheckReport(OverallStatus overallStatus,
                   List<VerifiedFinding> findings,
                   List<DocumentOutcome> documentOutcomes,
                   List<UnreadQuery> unreadQueries,
                   ReportMetadata metadata) {

    CheckReport {
        Objects.requireNonNull(overallStatus, "overallStatus");
        findings = List.copyOf(findings);
        documentOutcomes = List.copyOf(documentOutcomes);
        unreadQueries = List.copyOf(unreadQueries);
        Objects.requireNonNull(metadata, "metadata");
    }

    List<ReportFinding> portFindings() {
        return findings.stream()
                .map(f -> new ReportFinding(f.condition(), f.outcome().storedValue(), f.evidence(), f.note()))
                .toList();
    }

    /** The outcomes WITHOUT content (REQ-CHK-047). */
    List<ReportDocumentOutcome> portDocumentOutcomes() {
        return documentOutcomes.stream()
                .map(d -> new ReportDocumentOutcome(d.documentType(), d.sourceMode(), d.readStatus(), d.reason(), d.detail()))
                .toList();
    }

    List<ReportUnreadQuery> portUnreadQueries() {
        return unreadQueries.stream()
                .map(q -> new ReportUnreadQuery(q.queryName(), q.detail()))
                .toList();
    }
}
