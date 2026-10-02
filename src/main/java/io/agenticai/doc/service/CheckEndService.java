package io.agenticai.doc.service;

import io.agenticai.doc.contract.EndCheckResult;
import io.agenticai.doc.repository.UploadedDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * CON-DOC-005 — the end of a Check (SVC-API endCheck, steps 1 → 4). Honours: CON-DOC-005.
 * Records the Ended Check ({@link EndedCheckRecorder}, step 1), hard-deletes the Check's Uploaded
 * Documents (step 2, REQ-DOC-054, ADR-DOC-008), sweeps the Uploaded Documents of every Check
 * recorded as ended (step 3, REQ-DOC-062, ADR-DOC-015) and answers the rows deleted (step 4).
 * Idempotent: a second call records nothing new and deletes 0 rows. Raises no refusal.
 *
 * <p>Steps 2 to 4 run in this READ_WRITE transaction; step 1 commits in the recorder's own,
 * for the reason stated there. A concurrent duplicate record is treated as already recorded and
 * the deletes go on.
 */
@Service
public class CheckEndService {

    private static final Logger log = LoggerFactory.getLogger(CheckEndService.class);

    private final EndedCheckRecorder recorder;
    private final UploadedDocumentRepository uploads;

    public CheckEndService(EndedCheckRecorder recorder, UploadedDocumentRepository uploads) {
        this.recorder = Objects.requireNonNull(recorder, "recorder");
        this.uploads = Objects.requireNonNull(uploads, "uploads");
    }

    /** Ends the Check — see {@code DocumentAccess#endCheck}. */
    @Transactional
    public EndCheckResult endCheck(Long checkId) {
        Objects.requireNonNull(checkId, "checkId");
        // 1 — record the end (REQ-DOC-060)
        boolean recorded;
        try {
            recorded = recorder.record(checkId);
        } catch (DataIntegrityViolationException concurrentDuplicate) {
            // UQ_DOC_ENDED_CHECK_CHECK_ID: a concurrent end of the same Check recorded it first — already recorded
            log.debug("DOC ended Check recorded concurrently checkId={}", checkId, concurrentDuplicate);
            recorded = false;
        }
        // 2 — the Check's own uploads (REQ-DOC-054)
        int own = uploads.deleteByCheckId(checkId);
        // 3 — the sweep of late uploads of every ended Check (REQ-DOC-062)
        int swept = uploads.deleteOfEndedChecks();
        // 4
        int deletedCount = own + swept;
        log.info("DOC check ended checkId={} recorded={} deletedCount={} (own={}, swept={})",
                checkId, recorded, deletedCount, own, swept);
        return new EndCheckResult(deletedCount);
    }
}
