package io.agenticai.chk.port;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * CHK's immutable copy of one service package version (CON-REG-002 … CON-REG-004), loaded once at
 * a Check's start or on resuming a {@code manual} Check and kept in that Check's working data
 * only (RULE-CHK-007, REQ-CHK-008, REQ-CHK-065). Never stored, cached or logged (G9).
 *
 * @param serviceCode             the service code
 * @param versionNumber           the version number
 * @param serviceKnowledge        the whole service knowledge, unaltered (REQ-CHK-034; RULE-CHK-006
 *                                data source)
 * @param inputName               the version's input name — the one bind parameter (RULE-CHK-003)
 * @param fetchMode               the fetch mode code: {@code path}, {@code blob} or {@code manual}
 * @param documentSourceQueryName the document source query name; {@code null} when none
 *                                (RULE-CHK-005)
 * @param queries                 the version's service queries in their stored order; unmodifiable
 * @param requiredDocumentTypes   the required document types, compared exactly as stored
 *                                (RULE-CHK-004); unmodifiable
 */
public record ServicePackageSnapshot(String serviceCode,
                                     int versionNumber,
                                     String serviceKnowledge,
                                     String inputName,
                                     String fetchMode,
                                     String documentSourceQueryName,
                                     List<VersionQuery> queries,
                                     Set<String> requiredDocumentTypes) {

    /** The fetch mode whose Check waits for the employee's uploads (REQ-CHK-056). */
    public static final String MANUAL = "manual";

    public ServicePackageSnapshot {
        Objects.requireNonNull(serviceCode, "serviceCode");
        Objects.requireNonNull(serviceKnowledge, "serviceKnowledge");
        Objects.requireNonNull(inputName, "inputName");
        Objects.requireNonNull(fetchMode, "fetchMode");
        queries = List.copyOf(Objects.requireNonNull(queries, "queries"));
        requiredDocumentTypes = requiredDocumentTypes == null
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(requiredDocumentTypes));
    }

    /** Whether the Check waits in AWAITING_DOCUMENTS for the employee's uploads (ADR-CHK-004). */
    public boolean isManual() {
        return MANUAL.equals(fetchMode);
    }
}
