package io.agenticai.chk.port;

/**
 * The document fetch of a Check answered a failure instead of document outcomes — for example
 * "service package version not found" (REQ-CHK-023). Not a catalog error and not a
 * {@code CheckEngineException}: it never reaches a caller as a ProblemDetail. The pipeline turns
 * it into a pipeline outcome — the Check ends FAILED with reason INTERNAL_ERROR and this
 * exception's message as detail (REQ-CHK-023, ADR-CHK-018). The original failure is kept as the
 * cause.
 */
public class DocumentFetchFailedException extends RuntimeException {

    /**
     * @param failureText the failure text Document Access gave, used as the Check's detail
     * @param cause       the original failure
     */
    public DocumentFetchFailedException(String failureText, Throwable cause) {
        super(failureText, cause);
    }
}
