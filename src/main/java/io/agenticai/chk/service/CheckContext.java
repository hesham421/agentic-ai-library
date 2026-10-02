package io.agenticai.chk.service;

import io.agenticai.chk.port.ComparisonOutput;
import io.agenticai.chk.port.DocumentOutcome;
import io.agenticai.chk.port.ServicePackageSnapshot;
import io.agenticai.chk.port.UnreadQuery;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The working data of ONE running Check (REQ-CHK-063, REQ-CHK-065; ADR-CHK-011 (3)): its pinned
 * service package version, its query results, its document outcomes and content, and the model's
 * output. Created when the pipeline is launched, owned by that pipeline run alone, never shared,
 * stored, cached or logged (G9), and {@linkplain #clear() cleared} when {@code CheckPipeline.run}
 * returns, after which nothing references it. Module-internal; not thread-safe by design — exactly
 * one pipeline thread uses it.
 */
final class CheckContext {

    private final Long checkId;
    private final String requestNumber;
    private final String employeeId;
    private final OffsetDateTime startedAt;
    private final Instant deadline;
    private ServicePackageSnapshot servicePackage;

    private final Map<String, List<Map<String, Object>>> queryResults = new LinkedHashMap<>();
    private final List<UnreadQuery> unreadQueries = new ArrayList<>();
    private List<DocumentOutcome> documents = List.of();
    private ComparisonOutput modelOutput;

    private CheckContext(Long checkId, ServicePackageSnapshot servicePackage, String requestNumber,
                         String employeeId, OffsetDateTime startedAt, Instant deadline) {
        this.checkId = checkId;
        this.servicePackage = servicePackage;
        this.requestNumber = requestNumber;
        this.employeeId = employeeId;
        this.startedAt = startedAt;
        this.deadline = deadline;
    }

    /**
     * @param checkId        the Check identifier
     * @param servicePackage the version pinned at the Check's start (RULE-CHK-007)
     * @param requestNumber  the request number exactly as received
     * @param employeeId     the employee identity exactly as received
     * @param startedAt      the Check's start time (report metadata)
     * @param deadline       the end of the Check timeout counted from RUNNING (REQ-CHK-052)
     */
    static CheckContext of(Long checkId, ServicePackageSnapshot servicePackage, String requestNumber,
                           String employeeId, OffsetDateTime startedAt, Instant deadline) {
        return new CheckContext(
                Objects.requireNonNull(checkId, "checkId"),
                Objects.requireNonNull(servicePackage, "servicePackage"),
                Objects.requireNonNull(requestNumber, "requestNumber"),
                Objects.requireNonNull(employeeId, "employeeId"),
                Objects.requireNonNull(startedAt, "startedAt"),
                Objects.requireNonNull(deadline, "deadline"));
    }

    Long checkId() {
        return checkId;
    }

    ServicePackageSnapshot servicePackage() {
        return servicePackage;
    }

    String requestNumber() {
        return requestNumber;
    }

    String employeeId() {
        return employeeId;
    }

    OffsetDateTime startedAt() {
        return startedAt;
    }

    Instant deadline() {
        return deadline;
    }

    /** Whether the Check's own deadline has been reached (REQ-CHK-051, REQ-CHK-052). */
    boolean deadlinePassed(Instant now) {
        return !now.isBefore(deadline);
    }

    Map<String, List<Map<String, Object>>> queryResults() {
        return queryResults;
    }

    List<UnreadQuery> unreadQueries() {
        return unreadQueries;
    }

    List<DocumentOutcome> documents() {
        return documents;
    }

    void documents(List<DocumentOutcome> outcomes) {
        this.documents = List.copyOf(Objects.requireNonNull(outcomes, "outcomes"));
    }

    void modelOutput(ComparisonOutput output) {
        this.modelOutput = Objects.requireNonNull(output, "output");
    }

    ComparisonOutput modelOutput() {
        return modelOutput;
    }

    /** Drops every piece of the Check's working data (REQ-CHK-065). */
    void clear() {
        servicePackage = null;
        queryResults.clear();
        unreadQueries.clear();
        documents = List.of();
        modelOutput = null;
    }
}
