package io.agenticai.chk.domain;

import io.agenticai.chk.error.CheckEngineTexts;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * RULE-CHK-004 — the finding of each required document type, decided from the Document Access
 * outcomes of that type (REQ-CHK-019 … REQ-CHK-022, ADR-CHK-014): SATISFIED with at least one
 * READ, otherwise UNDETERMINED with at least one UNREADABLE (the reasons and details as evidence),
 * otherwise NOT_SATISFIED (evidence {@code MISSING}). Types are compared exactly as stored
 * (case-sensitive). The rule's message is the finding's note.
 *
 * <p>Plain Java, no state: one static operation.
 */
public final class RequiredDocumentRule {

    /** The message key of RULE-CHK-004 in {@code messages.properties}. */
    static final String RULE_004 = "CHK-RULE-004";

    /** CON-DOC-001 read status codes, as Document Access gives them. */
    static final String READ = "READ";
    static final String MISSING = "MISSING";
    static final String UNREADABLE = "UNREADABLE";

    private RequiredDocumentRule() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    /**
     * @param requiredDocumentTypes the version's required document types
     * @param readings              every document outcome of the Check
     * @return one finding per required type, in the types' natural order; unmodifiable
     */
    public static List<VerifiedFinding> decide(Set<String> requiredDocumentTypes, List<DocumentReading> readings) {
        Objects.requireNonNull(requiredDocumentTypes, "requiredDocumentTypes");
        Objects.requireNonNull(readings, "readings");
        List<VerifiedFinding> findings = new ArrayList<>(requiredDocumentTypes.size());
        for (String type : new TreeSet<>(requiredDocumentTypes)) {
            findings.add(decideOne(type, readings));
        }
        return List.copyOf(findings);
    }

    private static VerifiedFinding decideOne(String type, List<DocumentReading> readings) {
        List<DocumentReading> unreadable = new ArrayList<>();
        for (DocumentReading reading : readings) {
            if (!type.equals(reading.documentType())) {
                continue;
            }
            if (READ.equals(reading.readStatus())) {
                return finding(type, FindingOutcome.SATISFIED, READ, READ, "");
            }
            if (UNREADABLE.equals(reading.readStatus())) {
                unreadable.add(reading);
            }
        }
        if (!unreadable.isEmpty()) {
            List<String> reasons = unreadable.stream().map(DocumentReading::reason).toList();
            String evidence = String.join("; ", unreadable.stream()
                    .map(reading -> reading.reason() + ": " + reading.detail())
                    .toList());
            return finding(type, FindingOutcome.UNDETERMINED, UNREADABLE, evidence,
                    " (" + String.join(", ", reasons) + ")");
        }
        return finding(type, FindingOutcome.NOT_SATISFIED, MISSING, MISSING, "");
    }

    private static VerifiedFinding finding(String type, FindingOutcome outcome, String readStatus,
                                           String evidence, String reasonSuffix) {
        return new VerifiedFinding(type, outcome, evidence,
                CheckEngineTexts.english(RULE_004, type, readStatus, reasonSuffix));
    }
}
