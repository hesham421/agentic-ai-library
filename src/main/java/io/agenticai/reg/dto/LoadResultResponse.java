package io.agenticai.reg.dto;

import io.agenticai.reg.entity.LoadResult;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * The API document's {@code LoadResult} schema (API-REG-003), field for field: one outcome of the
 * latest load run. {@code loadResultId} is the one identifier the document exposes; the audit
 * columns are not part of the schema. {@code serviceCode}, {@code versionNumber} and
 * {@code reason} are {@code null} when the schema allows it. A plain JSON object — no envelope.
 * Documented here in Javadoc: springdoc is not on the classpath.
 *
 * @param loadResultId  the Load Result identifier (DBF-REG-040)
 * @param loadRunAt     the start of the load run, ISO-8601 date-time with offset (DBF-REG-041)
 * @param subjectKind   {@code SERVICE_PACKAGE}, {@code CONNECTION} or {@code PACKAGE_DIRECTORY} (DBF-REG-042)
 * @param subjectName   the folder name, connection name or package directory path, at most 200
 *                      characters (DBF-REG-043)
 * @param serviceCode   the service code as declared, at most 100 characters, or {@code null} (DBF-REG-044)
 * @param versionNumber the version number as declared, or {@code null} (DBF-REG-045)
 * @param outcome       {@code REGISTERED}, {@code UNCHANGED}, {@code REJECTED}, {@code WITHDRAWN},
 *                      {@code ACTIVATED}, {@code UPDATED} or {@code REMOVED} (DBF-REG-046)
 * @param reason        the rule message of a {@code REJECTED} outcome, at most 1000 characters, or
 *                      {@code null} (DBF-REG-047)
 */
public record LoadResultResponse(long loadResultId,
                                 OffsetDateTime loadRunAt,
                                 String subjectKind,
                                 String subjectName,
                                 String serviceCode,
                                 Integer versionNumber,
                                 String outcome,
                                 String reason) {

    public LoadResultResponse {
        Objects.requireNonNull(loadRunAt, "loadRunAt");
        Objects.requireNonNull(subjectKind, "subjectKind");
        Objects.requireNonNull(subjectName, "subjectName");
        Objects.requireNonNull(outcome, "outcome");
    }

    /** The one mapping of a Load Result row to the response (A.3.8). No decision is taken here. */
    public static LoadResultResponse of(LoadResult row) {
        Objects.requireNonNull(row, "row");
        return new LoadResultResponse(
                row.getLoadResultId(),
                row.getLoadRunAt(),
                row.getSubjectKind().storedValue(),
                row.getSubjectName(),
                row.getServiceCode(),
                row.getVersionNumber(),
                row.getOutcome().storedValue(),
                row.getReason());
    }
}
