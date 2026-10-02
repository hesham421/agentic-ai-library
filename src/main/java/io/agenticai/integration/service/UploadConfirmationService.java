package io.agenticai.integration.service;

import io.agenticai.chk.contract.ConfirmedCheck;
import io.agenticai.integration.dto.ConfirmedCheckResponse;
import io.agenticai.integration.port.CheckEnginePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * API-INT-003 — Confirm the uploads (CON-INT-003). Parse → {@link CheckEnginePort#confirm} → answer
 * at once; the pipeline continues in the background inside the Check Engine. The Check Engine's
 * refusals ({@code CHK-404-CHECK-NOT-FOUND}, {@code CHK-409-CHECK-NOT-AWAITING-DOCUMENTS}) pass
 * through unchanged; its locking read serialises concurrent confirmations — INT adds no guard.
 *
 * <p>No {@code @Transactional}: INT owns no table; the Check Engine's operation is its own transaction.
 */
@Service("intUploadConfirmationService")
public class UploadConfirmationService {

    private static final Logger log = LoggerFactory.getLogger(UploadConfirmationService.class);

    private final CheckEnginePort checkEngine;

    public UploadConfirmationService(CheckEnginePort checkEngine) {
        this.checkEngine = Objects.requireNonNull(checkEngine, "checkEngine");
    }

    /** Confirms that the uploads of the {@code manual} Check {@code checkId} are complete. */
    public ConfirmedCheckResponse confirm(Long checkId) {
        Objects.requireNonNull(checkId, "checkId");
        ConfirmedCheck confirmed = checkEngine.confirm(checkId);
        log.info("INT uploads confirmed checkId={} status={}", confirmed.checkId(), confirmed.status());
        return ConfirmedCheckResponse.of(confirmed);
    }
}
