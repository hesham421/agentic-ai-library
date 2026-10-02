package io.agenticai.chk.port;

/**
 * The comparison model is unavailable (REQ-CHK-053): no comparison model is configured, or the
 * provider could not be reached, answered an error or gave no answer. The pipeline ends the Check
 * FAILED with reason MODEL_UNAVAILABLE.
 *
 * <p>Not a catalog error and not a {@code CheckEngineException}: it never reaches a caller as a
 * ProblemDetail. The pipeline turns it into the Check's failure reason, with this exception's
 * message as detail (ADR-CHK-018). The message names the failure only — never the service
 * knowledge, the Check's data or the model's output. An underlying failure is kept as the cause.
 */
public class ModelUnavailableException extends RuntimeException {

    /**
     * @param message the failure, used as the Check's detail
     */
    public ModelUnavailableException(String message) {
        super(message);
    }

    /**
     * @param message the failure, used as the Check's detail
     * @param cause   the underlying failure
     */
    public ModelUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
