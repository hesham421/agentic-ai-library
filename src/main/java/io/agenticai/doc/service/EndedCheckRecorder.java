package io.agenticai.doc.service;

import io.agenticai.doc.entity.EndedCheck;
import io.agenticai.doc.repository.EndedCheckRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Step 1 of the end of a Check (SVC-API endCheck; REQ-DOC-060, ADR-DOC-015): the Check's
 * identifier recorded as an Ended Check unless it already is — the unit's
 * {@code MERGE … WHEN NOT MATCHED THEN INSERT}, written with JPA as an existence check and the
 * insert of a new instance, backstopped by {@code UQ_DOC_ENDED_CHECK_CHECK_ID}.
 *
 * <p>Why a transaction of its own ({@code REQUIRES_NEW}): a concurrent duplicate insert is
 * refused by the unique constraint, and the JPA specification marks the transaction in which a
 * persistence exception occurs for rollback only — the deletes of steps 2 and 3 could then never
 * commit alongside it. In its own transaction the refusal rolls back only this insert; the
 * caller treats it as "already recorded" and goes on to the deletes. The marker therefore commits
 * just before the deletes rather than with them; a handover that reads it is refused
 * (RULE-DOC-009), and an upload left between the two is removed by the sweep of this or the
 * next end of a Check (REQ-DOC-062).
 */
@Service
public class EndedCheckRecorder {

    private static final Logger log = LoggerFactory.getLogger(EndedCheckRecorder.class);

    private final EndedCheckRepository endedChecks;

    public EndedCheckRecorder(EndedCheckRepository endedChecks) {
        this.endedChecks = Objects.requireNonNull(endedChecks, "endedChecks");
    }

    /**
     * Records the Check as ended unless it already is.
     *
     * @return {@code true} when a row was inserted, {@code false} when the Check was already
     *         recorded before this call
     * @throws DataIntegrityViolationException {@code UQ_DOC_ENDED_CHECK_CHECK_ID} — a concurrent
     *                                         end of the same Check recorded it first; this
     *                                         transaction is rolled back and the caller treats
     *                                         the Check as already recorded (ADR-DOC-015)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean record(Long checkId) {
        Objects.requireNonNull(checkId, "checkId");
        if (endedChecks.existsByCheckId(checkId)) {
            log.debug("DOC ended Check already recorded checkId={}", checkId);
            return false;
        }
        endedChecks.saveAndFlush(EndedCheck.recorded(checkId));
        return true;
    }
}
