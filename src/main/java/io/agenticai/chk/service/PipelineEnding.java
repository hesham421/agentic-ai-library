package io.agenticai.chk.service;

import io.agenticai.chk.domain.CheckFailureReason;

/**
 * The one way a pipeline run ends its Check — implemented by {@link CheckEndingService} (the
 * single ending path, REQ-CHK-078, REQ-CHK-079) and, only inside {@link ModelEvaluationRunner}, by
 * an in-memory capture. Module-internal.
 */
interface PipelineEnding {

    /** Ends the Check COMPLETED with its report (REQ-CHK-044). */
    void complete(Long checkId, CheckReport report);

    /** Ends the Check FAILED with exactly one reason and a detail (REQ-CHK-054). */
    void fail(Long checkId, CheckFailureReason reason, String detail);
}
