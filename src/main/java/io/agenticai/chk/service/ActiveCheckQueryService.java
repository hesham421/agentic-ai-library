package io.agenticai.chk.service;

import io.agenticai.chk.dto.ActiveCheckView;
import io.agenticai.chk.error.CheckEngineErrorCodes;
import io.agenticai.chk.error.CheckEngineException;
import io.agenticai.chk.repository.ActiveCheckRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * API-CHK-001 — the Active Check of one Check: validate checkId → load the row (QR-CHK-001) → map
 * to {@link ActiveCheckView}; writes nothing (REQ-CHK-076, REQ-CHK-077, REQ-CHK-080). READ_ONLY.
 * No row — the Check has ended or does not exist — → {@code CHK-404-ACTIVE-CHECK-NOT-FOUND}.
 */
@Service
@Transactional(readOnly = true)
public class ActiveCheckQueryService {

    private static final Logger log = LoggerFactory.getLogger(ActiveCheckQueryService.class);

    private final ActiveCheckRepository activeChecks;

    public ActiveCheckQueryService(ActiveCheckRepository activeChecks) {
        this.activeChecks = Objects.requireNonNull(activeChecks, "activeChecks");
    }

    /**
     * @param checkId the Check identifier (the controller's binding guarantees a number)
     * @throws CheckEngineException {@code CHK-404-ACTIVE-CHECK-NOT-FOUND}
     */
    public ActiveCheckView findActiveCheck(Long checkId) {
        Objects.requireNonNull(checkId, "checkId");
        log.debug("CHK read Active Check checkId={}", checkId);
        return activeChecks.findRowByCheckId(checkId)
                .map(ActiveCheckView::of)
                .orElseThrow(() -> new CheckEngineException(CheckEngineErrorCodes.ACTIVE_CHECK_NOT_FOUND, checkId));
    }
}
