package io.agenticai.chk.service;

import io.agenticai.chk.domain.CheckFailureReason;
import io.agenticai.chk.error.CheckEngineErrorCodes;
import io.agenticai.chk.error.CheckEngineTexts;
import io.agenticai.chk.port.ComparisonTimedOutException;
import io.agenticai.chk.port.DocumentFetchFailedException;
import io.agenticai.chk.port.ModelNotPermittedException;
import io.agenticai.chk.port.ModelOutputInvalidException;
import io.agenticai.chk.port.ModelUnavailableException;

/**
 * The failure reason and detail of a pipeline exception (SVC-API pipeline, REQ-CHK-010):
 * <ul>
 *   <li>{@link ModelNotPermittedException} → MODEL_NOT_PERMITTED (REQ-CHK-073)</li>
 *   <li>{@link ModelUnavailableException} → MODEL_UNAVAILABLE (REQ-CHK-053)</li>
 *   <li>{@link ModelOutputInvalidException} → MODEL_OUTPUT_INVALID (REQ-CHK-039)</li>
 *   <li>{@link ComparisonTimedOutException} → TIMED_OUT (REQ-CHK-051)</li>
 *   <li>{@link DocumentFetchFailedException} → INTERNAL_ERROR, the failure text as detail
 *       (REQ-CHK-023)</li>
 *   <li>anything else → INTERNAL_ERROR with the catalog's unexpected-failure text — never the
 *       exception's own message, which could carry data (REQ-CHK-010)</li>
 * </ul>
 * The port exceptions' messages name the failure only, never the Check's data (their contract).
 *
 * @param reason the failure reason
 * @param detail the detail text, never empty
 */
record PipelineFailure(CheckFailureReason reason, String detail) {

    static PipelineFailure of(RuntimeException failure) {
        return switch (failure) {
            case ModelNotPermittedException e -> withMessage(CheckFailureReason.MODEL_NOT_PERMITTED, e);
            case ModelUnavailableException e -> withMessage(CheckFailureReason.MODEL_UNAVAILABLE, e);
            case ModelOutputInvalidException e -> withMessage(CheckFailureReason.MODEL_OUTPUT_INVALID, e);
            case ComparisonTimedOutException e -> withMessage(CheckFailureReason.TIMED_OUT, e);
            case DocumentFetchFailedException e -> withMessage(CheckFailureReason.INTERNAL_ERROR, e);
            default -> unexpected();
        };
    }

    /** INTERNAL_ERROR with the catalog's unexpected-failure text. */
    static PipelineFailure unexpected() {
        return new PipelineFailure(CheckFailureReason.INTERNAL_ERROR,
                CheckEngineTexts.english(CheckEngineErrorCodes.UNEXPECTED_FAILURE));
    }

    /** TIMED_OUT, the Check's own deadline reached (REQ-CHK-051); the reason code is the detail. */
    static PipelineFailure timedOut() {
        return new PipelineFailure(CheckFailureReason.TIMED_OUT, CheckFailureReason.TIMED_OUT.storedValue());
    }

    private static PipelineFailure withMessage(CheckFailureReason reason, RuntimeException failure) {
        String message = failure.getMessage();
        return new PipelineFailure(reason, message == null || message.isBlank() ? reason.storedValue() : message);
    }
}
