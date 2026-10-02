package io.agenticai.chk.domain;

import io.agenticai.chk.error.CheckEngineTexts;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Deterministic checks, part 2 — verification of the comparison model's findings (ADR-CHK-003),
 * each finding independently, in this order:
 * <ol>
 *   <li>no evidence → UNDETERMINED (REQ-CHK-040);</li>
 *   <li>evidence not found in the Check's own data — an exact substring once runs of whitespace are
 *       taken as one space — → UNDETERMINED (REQ-CHK-029);</li>
 *   <li>an explicit condition: value found not in the data → UNDETERMINED (REQ-CHK-026); limit
 *       not literally in the service knowledge → UNDETERMINED with RULE-CHK-006's message as note
 *       (REQ-CHK-027); value or limit not computable → UNDETERMINED (REQ-CHK-028); otherwise the
 *       comparison recomputed by {@link ExplicitComparison} replaces the model's outcome
 *       (REQ-CHK-024, REQ-CHK-025);</li>
 *   <li>otherwise the model's outcome stands.</li>
 * </ol>
 *
 * <p>A plain domain class (no Spring, no I/O): every fact is passed in. Built per Check by
 * {@link #create}; it holds that Check's service knowledge and data texts only for the Check's
 * verification and is dropped with the Check's working data (G9).
 */
public final class FindingVerifier {

    /** The message key of RULE-CHK-006 in {@code messages.properties}. */
    static final String RULE_006 = "CHK-RULE-006";

    /** The message key of the note of a finding the model gave no evidence for (REQ-CHK-040). */
    static final String REQ_040 = "CHK-REQ-040";

    /**
     * The evidence recorded for a finding the model gave no evidence for (REQ-CHK-040): RPT stores
     * a finding only with a non-blank evidence (RULE-RPT-007), as for the required-document
     * findings' status words of {@link RequiredDocumentRule}.
     */
    static final String NO_EVIDENCE = "NONE";

    private final String serviceKnowledge;
    private final List<String> dataTexts;

    private FindingVerifier(String serviceKnowledge, List<String> dataTexts) {
        this.serviceKnowledge = serviceKnowledge;
        this.dataTexts = dataTexts;
    }

    /**
     * @param serviceKnowledge the whole service knowledge of the Check's version (RULE-CHK-006)
     * @param dataTexts        the Check's own data as texts: its read query results and the content
     *                         of its READ documents
     */
    public static FindingVerifier create(String serviceKnowledge, List<String> dataTexts) {
        Objects.requireNonNull(serviceKnowledge, "serviceKnowledge");
        Objects.requireNonNull(dataTexts, "dataTexts");
        return new FindingVerifier(serviceKnowledge,
                dataTexts.stream().map(FindingVerifier::normalised).toList());
    }

    /**
     * Verifies one finding of the model.
     *
     * @param condition the condition, as the model stated it
     * @param claimed   the model's outcome
     * @param evidence  the model's evidence; may be {@code null}
     * @param note      the model's note; may be {@code null}
     * @param explicit  the explicit condition; {@code null} for any other condition
     * @return the finding with the outcome code decides
     */
    public VerifiedFinding verify(String condition,
                                  FindingOutcome claimed,
                                  String evidence,
                                  String note,
                                  ExplicitCondition explicit) {
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(claimed, "claimed");
        if (evidence == null || evidence.isBlank()) {
            return new VerifiedFinding(condition, FindingOutcome.UNDETERMINED, NO_EVIDENCE,
                    note == null || note.isBlank() ? CheckEngineTexts.english(REQ_040) : note);
        }
        if (!inData(evidence)) {
            return new VerifiedFinding(condition, FindingOutcome.UNDETERMINED, evidence, note);
        }
        if (explicit == null) {
            return new VerifiedFinding(condition, claimed, evidence, note);
        }
        if (explicit.valueFound() == null || explicit.valueFound().isBlank() || !inData(explicit.valueFound())) {
            return new VerifiedFinding(condition, FindingOutcome.UNDETERMINED, evidence, note);
        }
        if (explicit.limit() == null || explicit.limit().isBlank() || !serviceKnowledge.contains(explicit.limit())) {
            return new VerifiedFinding(condition, FindingOutcome.UNDETERMINED, evidence,
                    CheckEngineTexts.english(RULE_006, String.valueOf(explicit.limit())));
        }
        Optional<Boolean> holds = ExplicitComparison.evaluate(
                explicit.valueFound(), explicit.comparison(), explicit.limit());
        FindingOutcome outcome = holds
                .map(met -> met ? FindingOutcome.SATISFIED : FindingOutcome.NOT_SATISFIED)
                .orElse(FindingOutcome.UNDETERMINED);
        return new VerifiedFinding(condition, outcome, evidence, note);
    }

    private boolean inData(String text) {
        String wanted = normalised(text);
        if (wanted.isEmpty()) {
            return false;
        }
        for (String data : dataTexts) {
            if (data.contains(wanted)) {
                return true;
            }
        }
        return false;
    }

    private static String normalised(String text) {
        return text.trim().replaceAll("\\s+", " ");
    }
}
