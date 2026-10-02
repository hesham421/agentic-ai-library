package io.agenticai.chk.port;

/**
 * The comparison ran out of time (REQ-CHK-051): the Check's deadline was reached before the
 * comparison call, or while it ran (the call is then cancelled), or the pipeline was interrupted
 * — which is how the deadline check stops a RUNNING Check. The pipeline ends the Check FAILED
 * with reason TIMED_OUT, exactly as for its own deadline test before each step; when the deadline
 * check ended the Check first, the ending finds the Active Check gone and stops (REQ-CHK-079).
 *
 * <p>Not a catalog error and not a {@code CheckEngineException}: it never reaches a caller as a
 * ProblemDetail. The pipeline turns it into the Check's failure reason, with this exception's
 * message as detail (ADR-CHK-018). The message names the failure only — never the service
 * knowledge, the Check's data or the model's output. An underlying failure is kept as the cause.
 */
public class ComparisonTimedOutException extends RuntimeException {

    /**
     * @param message the failure, used as the Check's detail
     */
    public ComparisonTimedOutException(String message) {
        super(message);
    }

    /**
     * @param message the failure, used as the Check's detail
     * @param cause   the underlying failure
     */
    public ComparisonTimedOutException(String message, Throwable cause) {
        super(message, cause);
    }
}
