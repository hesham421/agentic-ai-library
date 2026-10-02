package io.agenticai.chk.port;

/**
 * The comparison model's answer does not fit the fixed report structure (REQ-CHK-039): it is not
 * JSON, does not parse into {@link ComparisonOutput}, or carries no findings list. The pipeline
 * ends the Check FAILED with reason MODEL_OUTPUT_INVALID and no Overall Status.
 *
 * <p>Not a catalog error and not a {@code CheckEngineException}: it never reaches a caller as a
 * ProblemDetail. The pipeline turns it into the Check's failure reason, with this exception's
 * message as detail (ADR-CHK-018). The message names the failure only — never the service
 * knowledge, the Check's data or the model's output. An underlying failure is kept as the cause.
 */
public class ModelOutputInvalidException extends RuntimeException {

    /**
     * @param message the failure, used as the Check's detail
     */
    public ModelOutputInvalidException(String message) {
        super(message);
    }

    /**
     * @param message the failure, used as the Check's detail
     * @param cause   the underlying failure
     */
    public ModelOutputInvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}
