package io.agenticai.chk.service;

import io.agenticai.chk.port.DocumentContent;
import io.agenticai.chk.port.DocumentOutcome;
import io.agenticai.chk.port.ServicePackageSnapshot;
import io.agenticai.chk.port.VersionQuery;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One request of the known-result request set (REQ-CHK-069, ADR-CHK-012), read from one JSON file
 * of {@code model-eval/known-result-set/}. The plan states only its parts — synthetic request
 * data, synthetic document contents, its service package and its expected Overall Status — and no
 * file format; this record is the minimal documented format the runner reads (recorded as an
 * open API-document gap). Property names are the component names:
 *
 * <pre>{@code
 * {
 *   "request": "SYN-001", "synthetic": true, "expectedOverallStatus": "COMPLIANT",
 *   "servicePackage": {"serviceCode": "...", "versionNumber": 3, "serviceKnowledge": "...",
 *                      "inputName": "...", "fetchMode": "path", "documentSourceQueryName": "...",
 *                      "queries": [{"queryName": "...", "connectionName": "...", "sqlText": "..."}],
 *                      "requiredDocumentTypes": ["..."]},
 *   "queryResults": {"<queryName>": [{"<column>": "<value>"}]},
 *   "unreadQueries": {"<queryName>": "<detail>"},
 *   "documents": [{"documentType": "...", "readStatus": "READ", "reason": null, "detail": null,
 *                  "text": "..."}]
 * }
 * }</pre>
 *
 * Synthetic data only — never a real request (REQ-CHK-074).
 */
record KnownResultRequest(String request,
                          boolean synthetic,
                          String expectedOverallStatus,
                          KnownPackage servicePackage,
                          Map<String, List<Map<String, Object>>> queryResults,
                          Map<String, String> unreadQueries,
                          List<KnownDocument> documents) {

    KnownResultRequest {
        queryResults = queryResults == null ? Map.of() : queryResults;
        unreadQueries = unreadQueries == null ? Map.of() : unreadQueries;
        documents = documents == null ? List.of() : documents;
    }

    /** The request's service package version. */
    record KnownPackage(String serviceCode,
                        int versionNumber,
                        String serviceKnowledge,
                        String inputName,
                        String fetchMode,
                        String documentSourceQueryName,
                        List<VersionQuery> queries,
                        Set<String> requiredDocumentTypes) {

        ServicePackageSnapshot snapshot() {
            return new ServicePackageSnapshot(serviceCode, versionNumber, serviceKnowledge, inputName, fetchMode,
                    documentSourceQueryName, queries == null ? List.of() : queries,
                    requiredDocumentTypes == null ? Set.of() : requiredDocumentTypes);
        }
    }

    /** One synthetic document outcome; {@code text} is its content when READ. */
    record KnownDocument(String documentType, String readStatus, String reason, String detail, String text) {

        DocumentOutcome outcome(String sourceMode) {
            DocumentContent content = DocumentOutcome.READ.equals(readStatus) ? new DocumentContent.Text(text) : null;
            return new DocumentOutcome(documentType, sourceMode, readStatus, reason, detail, content);
        }
    }
}
