package io.agenticai.chk.port;

/**
 * The comparison model is not permitted (REQ-CHK-072, REQ-CHK-073): its tier is FREE and the
 * environment's data class is REAL, so nothing was sent to it (ADR-CHK-006, ADR-CHK-010). The
 * pipeline ends the Check FAILED with reason MODEL_NOT_PERMITTED.
 *
 * <p>Not a catalog error and not a {@code CheckEngineException}: it never reaches a caller as a
 * ProblemDetail. The pipeline turns it into the Check's failure reason, with this exception's
 * message as detail (ADR-CHK-018). The message names the failure only — never the service
 * knowledge, the Check's data or the model's output. An underlying failure is kept as the cause.
 */
public class ModelNotPermittedException extends RuntimeException {

    /**
     * @param message the failure, used as the Check's detail
     */
    public ModelNotPermittedException(String message) {
        super(message);
    }

    /**
     * @param message the failure, used as the Check's detail
     * @param cause   the underlying failure
     */
    public ModelNotPermittedException(String message, Throwable cause) {
        super(message, cause);
    }
}
