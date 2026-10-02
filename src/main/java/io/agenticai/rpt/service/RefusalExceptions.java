package io.agenticai.rpt.service;

import io.agenticai.rpt.contract.ApprovalFlagOnRejectionException;
import io.agenticai.rpt.contract.CheckEndedException;
import io.agenticai.rpt.contract.CheckNotCompletedException;
import io.agenticai.rpt.contract.CheckNotRunningException;
import io.agenticai.rpt.contract.CheckRunIncompleteException;
import io.agenticai.rpt.contract.CompliantNotVerifiedException;
import io.agenticai.rpt.contract.DecisionAlreadyRecordedException;
import io.agenticai.rpt.contract.DecisionIncompleteException;
import io.agenticai.rpt.contract.DocumentReasonMismatchException;
import io.agenticai.rpt.contract.FailureIncompleteException;
import io.agenticai.rpt.contract.FindingIncompleteException;
import io.agenticai.rpt.contract.InitialStatusMismatchException;
import io.agenticai.rpt.contract.MetadataMismatchException;
import io.agenticai.rpt.contract.RptRefusalException;
import io.agenticai.rpt.contract.UnknownCodeException;
import io.agenticai.rpt.domain.RuleRefusal;

import java.util.Objects;
import java.util.Optional;

/**
 * Turns a refusal decided by an RPT domain class ({@link RuleRefusal}) into the typed exception of
 * the SVC-API in-process rejection table — the one place where a {@code RefusalReason} meets its
 * code (ADR-RPT-012, ADR-RPT-013). It decides nothing: the domain class decided. Module-internal.
 */
final class RefusalExceptions {

    private RefusalExceptions() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    /**
     * Throws the exception of the refusal, if any.
     *
     * @param checkId the Check identifier the message names; {@code null} for {@code createCheckRun}
     *                and the received-value decision checks, whose messages name none
     */
    static void raise(Optional<RuleRefusal> refusal, Long checkId) {
        if (refusal.isPresent()) {
            throw of(refusal.get(), checkId);
        }
    }

    /** The exception of one refusal. */
    static RptRefusalException of(RuleRefusal refusal, Long checkId) {
        Objects.requireNonNull(refusal, "refusal");
        return switch (refusal.reason()) {
            case CHECK_RUN_INCOMPLETE -> new CheckRunIncompleteException(value(refusal, "field"));
            case UNKNOWN_CODE -> new UnknownCodeException(value(refusal, "value"), value(refusal, "lookupKey"));
            case AWAITING_NEEDS_MANUAL -> InitialStatusMismatchException.awaitingNeedsManual();
            case MANUAL_STARTS_AWAITING -> InitialStatusMismatchException.manualStartsAwaiting();
            case CHECK_ENDED -> new CheckEndedException(checkId);
            case CHECK_NOT_RUNNING -> new CheckNotRunningException(checkId);
            case METADATA_MISMATCH -> MetadataMismatchException.differs(checkId,
                    value(refusal, "field"), value(refusal, "value"), value(refusal, "stored"));
            case METADATA_MISSING -> MetadataMismatchException.missing(checkId, value(refusal, "field"));
            case FINDING_INCOMPLETE -> FindingIncompleteException.finding(checkId,
                    value(refusal, "position"), value(refusal, "field"));
            case UNREAD_QUERY_INCOMPLETE -> FindingIncompleteException.unreadQuery(checkId);
            case DOCUMENT_REASON_MISSING -> DocumentReasonMismatchException.reasonMissing(checkId,
                    value(refusal, "position"));
            case DOCUMENT_REASON_NOT_ALLOWED -> DocumentReasonMismatchException.reasonNotAllowed(checkId,
                    value(refusal, "position"), value(refusal, "readStatus"));
            case DOCUMENT_INCOMPLETE -> DocumentReasonMismatchException.documentTypeMissing(checkId);
            case COMPLIANT_NOT_VERIFIED -> new CompliantNotVerifiedException(checkId);
            case FAILURE_INCOMPLETE -> new FailureIncompleteException(checkId, value(refusal, "field"));
            case DECISION_ALREADY_RECORDED -> new DecisionAlreadyRecordedException(checkId);
            case CHECK_NOT_COMPLETED -> new CheckNotCompletedException(checkId);
            case DECISION_CODE_UNKNOWN -> DecisionIncompleteException.codeUnknown(value(refusal, "value"));
            case DECIDED_BY_MISSING -> DecisionIncompleteException.decidedByMissing();
            case APPROVAL_FLAG_MISSING -> DecisionIncompleteException.approvalFlagMissing();
            case APPROVAL_FLAG_ON_REJECTION -> new ApprovalFlagOnRejectionException();
        };
    }

    private static String value(RuleRefusal refusal, String placeholder) {
        return refusal.placeholders().get(placeholder);
    }
}
