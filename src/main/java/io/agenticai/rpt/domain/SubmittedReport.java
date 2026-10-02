package io.agenticai.rpt.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The report the Check Engine hands over with {@code completeCheck}, and the decision whether it
 * may be stored on its Check run (DATA-DOM, owner layer domain). This is the value object the
 * unit calls {@code CheckReport} (RULE-RPT-005); it is named {@code SubmittedReport} because
 * {@code CheckReport} is already the response of API-RPT-001 (SVC-API).
 *
 * <p>{@link #refusalAgainst(StoredRun)} checks the whole report in memory, before any write, in
 * the SVC-API order (ADR-RPT-012) and returns the first rule broken:
 * <ol>
 *   <li>RULE-RPT-006 — every code present: Overall Status, metadata fetch mode, each finding
 *       outcome, each document source mode and read status ("a null enum on a required field");
 *       → {@link RefusalReason#UNKNOWN_CODE};</li>
 *   <li>RULE-RPT-004 — metadata present and equal to the run: service code, version number,
 *       fetch mode, employee identity and start time equal those stored, a comparison model is
 *       present, the end time is present and not earlier than the start time →
 *       {@link RefusalReason#METADATA_MISSING} / {@link RefusalReason#METADATA_MISMATCH};</li>
 *   <li>RULE-RPT-007 — every finding carries a condition, an evidence and a note, not blank
 *       ({@link RefusalReason#FINDING_INCOMPLETE}); every unread query a name and a detail
 *       ({@link RefusalReason#UNREAD_QUERY_INCOMPLETE});</li>
 *   <li>RULE-RPT-008 — every document outcome carries a document type
 *       ({@link RefusalReason#DOCUMENT_INCOMPLETE}); a reason exactly when UNREADABLE
 *       ({@link RefusalReason#DOCUMENT_REASON_MISSING} /
 *       {@link RefusalReason#DOCUMENT_REASON_NOT_ALLOWED});</li>
 *   <li>RULE-RPT-005 — COMPLIANT only when every finding is SATISFIED and no service query is
 *       unread ({@link RefusalReason#COMPLIANT_NOT_VERIFIED}).</li>
 * </ol>
 * Positions are 1-based, in the order received (REQ-RPT-010 … REQ-RPT-012). Plain Java: no
 * framework, no I/O; it returns a decision, the caller raises the SVC-API exception. Whether the
 * run may be completed at all (RULE-RPT-003) is {@link CheckStatusTransition}'s.
 */
public final class SubmittedReport {

    /** One finding as received: condition, outcome, evidence, note (CON-CHK-008). */
    public record FindingItem(String condition, FindingOutcome outcome, String evidence, String note) {
    }

    /** One document outcome as received: type, source mode, read status, reason, detail (CON-CHK-008). */
    public record DocumentItem(String documentType, FetchMode sourceMode, DocumentReadStatus readStatus,
                               UnreadableReason reason, String detail) {
    }

    /** One unread service query as received: name and detail (CON-CHK-008). */
    public record UnreadQueryItem(String queryName, String detail) {
    }

    /** The report metadata as received (CON-CHK-008). */
    public record Metadata(String serviceCode, Integer versionNumber, FetchMode fetchMode,
                           String comparisonModel, String employeeId, OffsetDateTime startedAt,
                           OffsetDateTime endedAt) {
    }

    /** The values stored on the Check run that the metadata must equal (RULE-RPT-004). */
    public record StoredRun(String serviceCode, Integer versionNumber, FetchMode fetchMode,
                            String employeeId, OffsetDateTime startedAt) {

        public StoredRun {
            Objects.requireNonNull(serviceCode, "serviceCode");
            Objects.requireNonNull(versionNumber, "versionNumber");
            Objects.requireNonNull(fetchMode, "fetchMode");
            Objects.requireNonNull(employeeId, "employeeId");
            Objects.requireNonNull(startedAt, "startedAt");
        }
    }

    private final OverallStatus overallStatus;
    private final List<FindingItem> findings;
    private final List<DocumentItem> documents;
    private final List<UnreadQueryItem> unreadQueries;
    private final Metadata metadata;

    private SubmittedReport(OverallStatus overallStatus, List<FindingItem> findings,
                            List<DocumentItem> documents, List<UnreadQueryItem> unreadQueries,
                            Metadata metadata) {
        this.overallStatus = overallStatus;
        this.findings = findings;
        this.documents = documents;
        this.unreadQueries = unreadQueries;
        this.metadata = metadata;
    }

    /**
     * The report as received. The lists and their items must be present (API misuse otherwise);
     * the values inside may be absent — {@link #refusalAgainst(StoredRun)} decides.
     */
    public static SubmittedReport create(OverallStatus overallStatus,
                                         List<FindingItem> findings,
                                         List<DocumentItem> documents,
                                         List<UnreadQueryItem> unreadQueries,
                                         Metadata metadata) {
        return new SubmittedReport(overallStatus,
                List.copyOf(Objects.requireNonNull(findings, "findings")),
                List.copyOf(Objects.requireNonNull(documents, "documents")),
                List.copyOf(Objects.requireNonNull(unreadQueries, "unreadQueries")),
                metadata);
    }

    /** The first rule the report breaks against the stored run, or empty when it may be stored. */
    public Optional<RuleRefusal> refusalAgainst(StoredRun run) {
        Objects.requireNonNull(run, "run");
        return firstPresent(
                this::unknownCode,
                () -> metadataRefusal(run),
                this::incompleteFinding,
                this::incompleteUnreadQuery,
                this::documentRefusal,
                this::compliantRefusal);
    }

    // Evaluated lazily, in order: a later check relies on the earlier ones (codes present).
    @SafeVarargs
    private static Optional<RuleRefusal> firstPresent(Supplier<Optional<RuleRefusal>>... checks) {
        for (Supplier<Optional<RuleRefusal>> check : checks) {
            Optional<RuleRefusal> refusal = check.get();
            if (refusal.isPresent()) {
                return refusal;
            }
        }
        return Optional.empty();
    }

    // RULE-RPT-006 — a null enum on a required field
    private Optional<RuleRefusal> unknownCode() {
        if (overallStatus == null) {
            return unknown("OVERALL_STATUS");
        }
        if (metadata != null && metadata.fetchMode() == null) {
            return unknown("FETCH_MODE");
        }
        for (FindingItem finding : findings) {
            if (finding.outcome() == null) {
                return unknown("FINDING_OUTCOME");
            }
        }
        for (DocumentItem document : documents) {
            if (document.sourceMode() == null) {
                return unknown("FETCH_MODE");
            }
            if (document.readStatus() == null) {
                return unknown("DOCUMENT_READ_STATUS");
            }
        }
        return Optional.empty();
    }

    private static Optional<RuleRefusal> unknown(String lookupKey) {
        return Optional.of(RuleRefusal.of(RefusalReason.UNKNOWN_CODE, null, lookupKey));
    }

    // RULE-RPT-004
    private Optional<RuleRefusal> metadataRefusal(StoredRun run) {
        if (metadata == null) {
            return missing("metadata");
        }
        if (Texts.isBlank(metadata.serviceCode())) {
            return missing("serviceCode");
        }
        if (!metadata.serviceCode().equals(run.serviceCode())) {
            return mismatch("serviceCode", metadata.serviceCode(), run.serviceCode());
        }
        if (metadata.versionNumber() == null) {
            return missing("versionNumber");
        }
        if (!metadata.versionNumber().equals(run.versionNumber())) {
            return mismatch("versionNumber", metadata.versionNumber(), run.versionNumber());
        }
        if (metadata.fetchMode() != run.fetchMode()) {
            return mismatch("fetchMode", metadata.fetchMode().storedValue(),
                    run.fetchMode().storedValue());
        }
        if (Texts.isBlank(metadata.comparisonModel())) {
            return missing("comparisonModel");
        }
        if (Texts.isBlank(metadata.employeeId())) {
            return missing("employeeId");
        }
        if (!metadata.employeeId().equals(run.employeeId())) {
            return mismatch("employeeId", metadata.employeeId(), run.employeeId());
        }
        if (metadata.startedAt() == null) {
            return missing("startedAt");
        }
        if (!metadata.startedAt().isEqual(run.startedAt())) {
            return mismatch("startedAt", metadata.startedAt(), run.startedAt());
        }
        if (metadata.endedAt() == null) {
            return missing("endedAt");
        }
        if (metadata.endedAt().isBefore(run.startedAt())) {
            return mismatch("endedAt", metadata.endedAt(), run.startedAt());
        }
        return Optional.empty();
    }

    private static Optional<RuleRefusal> missing(String field) {
        return Optional.of(RuleRefusal.of(RefusalReason.METADATA_MISSING, field));
    }

    private static Optional<RuleRefusal> mismatch(String field, Object value, Object stored) {
        return Optional.of(RuleRefusal.of(RefusalReason.METADATA_MISMATCH, field, value, stored));
    }

    // RULE-RPT-007 — findings
    private Optional<RuleRefusal> incompleteFinding() {
        for (int i = 0; i < findings.size(); i++) {
            FindingItem finding = findings.get(i);
            String field = Texts.isBlank(finding.condition()) ? "condition"
                    : Texts.isBlank(finding.evidence()) ? "evidence"
                    : Texts.isBlank(finding.note()) ? "note"
                    : null;
            if (field != null) {
                return Optional.of(RuleRefusal.of(RefusalReason.FINDING_INCOMPLETE, i + 1, field));
            }
        }
        return Optional.empty();
    }

    // RULE-RPT-007 — unread queries (DATA-DOM ENT-RPT-004)
    private Optional<RuleRefusal> incompleteUnreadQuery() {
        for (int i = 0; i < unreadQueries.size(); i++) {
            UnreadQueryItem query = unreadQueries.get(i);
            String field = Texts.isBlank(query.queryName()) ? "queryName"
                    : Texts.isBlank(query.detail()) ? "detail"
                    : null;
            if (field != null) {
                return Optional.of(RuleRefusal.of(RefusalReason.UNREAD_QUERY_INCOMPLETE, i + 1, field));
            }
        }
        return Optional.empty();
    }

    // RULE-RPT-008
    private Optional<RuleRefusal> documentRefusal() {
        for (int i = 0; i < documents.size(); i++) {
            DocumentItem document = documents.get(i);
            int position = i + 1;
            if (Texts.isBlank(document.documentType())) {
                return Optional.of(RuleRefusal.of(RefusalReason.DOCUMENT_INCOMPLETE, position, "documentType"));
            }
            boolean unreadable = document.readStatus() == DocumentReadStatus.UNREADABLE;
            if (unreadable && document.reason() == null) {
                return Optional.of(RuleRefusal.of(RefusalReason.DOCUMENT_REASON_MISSING, position));
            }
            if (!unreadable && document.reason() != null) {
                return Optional.of(RuleRefusal.of(RefusalReason.DOCUMENT_REASON_NOT_ALLOWED,
                        position, document.readStatus().storedValue()));
            }
        }
        return Optional.empty();
    }

    // RULE-RPT-005
    private Optional<RuleRefusal> compliantRefusal() {
        if (overallStatus != OverallStatus.COMPLIANT) {
            return Optional.empty();
        }
        boolean everyFindingSatisfied = findings.stream()
                .allMatch(finding -> finding.outcome() == FindingOutcome.SATISFIED);
        if (!everyFindingSatisfied || !unreadQueries.isEmpty()) {
            return Optional.of(RuleRefusal.of(RefusalReason.COMPLIANT_NOT_VERIFIED));
        }
        return Optional.empty();
    }

    public OverallStatus overallStatus() {
        return overallStatus;
    }

    /** Unmodifiable, in the order received. */
    public List<FindingItem> findings() {
        return findings;
    }

    /** Unmodifiable, in the order received. */
    public List<DocumentItem> documents() {
        return documents;
    }

    /** Unmodifiable, in the order received. */
    public List<UnreadQueryItem> unreadQueries() {
        return unreadQueries;
    }

    public Metadata metadata() {
        return metadata;
    }
}
