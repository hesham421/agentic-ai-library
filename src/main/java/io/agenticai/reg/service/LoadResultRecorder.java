package io.agenticai.reg.service;

import io.agenticai.reg.domain.LoadOutcome;
import io.agenticai.reg.domain.LoadResultText;
import io.agenticai.reg.domain.LoadSubject;
import io.agenticai.reg.entity.LoadResult;
import io.agenticai.reg.repository.LoadResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * The single writer of Load Result rows (REQ-REG-007, REQ-REG-061, ADR-REG-007). Before every insert it fits
 * the subject name, service code and reason to their columns — 200 / 100 / 1000 characters, the
 * last kept character replaced by "…" (RULE-REG-028, REQ-REG-074, ADR-REG-022) — and renders the
 * reason as the rule's English message with its placeholders filled ({@link LoadReasonMessages}).
 * The load reason code itself is not stored (the report carries the message); it is logged at
 * debug. Runs inside the load run's transaction; nothing here decides an outcome.
 */
@Component
class LoadResultRecorder {

    private static final Logger log = LoggerFactory.getLogger(LoadResultRecorder.class);

    private final LoadResultRepository loadResults;

    LoadResultRecorder(LoadResultRepository loadResults) {
        this.loadResults = Objects.requireNonNull(loadResults, "loadResults");
    }

    /**
     * Records one outcome of the running load run.
     *
     * @param loadRunAt     the start of the load run
     * @param subjectKind   what the row is about
     * @param subjectName   the folder name, connection name or package directory path
     * @param serviceCode   the canonical service code as declared, or {@code null} when unknown
     * @param versionNumber the version number as declared, or {@code null} when unknown
     * @param outcome       the outcome
     * @param reason        the load reason of a {@code REJECTED} outcome; {@code null} otherwise
     * @throws IllegalArgumentException when {@code REJECTED} is recorded without a reason (API misuse)
     */
    void record(OffsetDateTime loadRunAt,
                LoadSubject subjectKind,
                String subjectName,
                String serviceCode,
                Integer versionNumber,
                LoadOutcome outcome,
                LoadReason reason) {
        if (outcome == LoadOutcome.REJECTED && reason == null) {
            throw new IllegalArgumentException("A REJECTED load result needs its load reason");
        }
        String reasonText = reason == null ? null : LoadReasonMessages.english(reason);
        LoadResult row = LoadResult.recorded(
                loadRunAt,
                subjectKind,
                LoadResultText.fit(subjectName, LoadResultText.SUBJECT_NAME_LIMIT),
                LoadResultText.fit(serviceCode, LoadResultText.SERVICE_CODE_LIMIT),
                versionNumber,
                outcome,
                LoadResultText.fit(reasonText, LoadResultText.REASON_LIMIT));
        loadResults.save(row);
        log.debug("REG load result kind={} subject={} outcome={} code={}",
                subjectKind, subjectName, outcome, reason == null ? "-" : reason.code());
    }

    /**
     * Records a {@code REJECTED} outcome whose reason has no load reason code in the catalog
     * (the interim cases of this unit's open {@code api_doc_gaps} row): the plain English text is
     * stored, fitted like any reason; the placeholder {@code REG-LOAD-?} appears in the log only.
     */
    void recordInterim(OffsetDateTime loadRunAt,
                       LoadSubject subjectKind,
                       String subjectName,
                       LoadOutcome outcome,
                       String reasonText) {
        Objects.requireNonNull(reasonText, "reasonText");
        LoadResult row = LoadResult.recorded(
                loadRunAt,
                subjectKind,
                LoadResultText.fit(subjectName, LoadResultText.SUBJECT_NAME_LIMIT),
                null,
                null,
                outcome,
                LoadResultText.fit(reasonText, LoadResultText.REASON_LIMIT));
        loadResults.save(row);
        log.debug("REG load result kind={} subject={} outcome={} code=REG-LOAD-? (interim, no code stated)",
                subjectKind, subjectName, outcome);
    }
}
