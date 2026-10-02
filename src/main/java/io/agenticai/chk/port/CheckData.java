package io.agenticai.chk.port;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The data part of one comparison call (REQ-CHK-035): the rows of every service query that was
 * read and the document outcomes of the Check. Only the Check's own data — nothing of another
 * Check (REQ-CHK-064). It lives in the Check's working data only and is never stored, cached or
 * logged (REQ-CHK-065, guardrail G9).
 *
 * <p>The comparison model receives the query results and the content of the READ documents
 * only; a MISSING or UNREADABLE outcome has no content and is decided in code (RULE-CHK-004), so
 * it is not sent.
 *
 * @param queryResults the rows of each read service query, keyed by query name in the order the
 *                     queries ran; each row maps a column name to its value as the query channel
 *                     returned it (a JSON-able value; {@code null} for an SQL NULL). An unread
 *                     query has no entry. Unmodifiable
 * @param documents    the document outcomes of the Check, as Document Access gave them;
 *                     unmodifiable
 */
public record CheckData(Map<String, List<Map<String, Object>>> queryResults,
                        List<DocumentOutcome> documents) {

    public CheckData {
        Objects.requireNonNull(queryResults, "queryResults");
        Map<String, List<Map<String, Object>>> results = new LinkedHashMap<>();
        queryResults.forEach((queryName, rows) -> results.put(
                Objects.requireNonNull(queryName, "queryName"),
                Objects.requireNonNull(rows, "rows").stream()
                        .map(row -> Collections.unmodifiableMap(new LinkedHashMap<>(row)))
                        .toList()));
        queryResults = Collections.unmodifiableMap(results);
        documents = List.copyOf(Objects.requireNonNull(documents, "documents"));
    }

    /** The READ documents — the only documents whose content goes to the model. */
    public List<DocumentOutcome> readDocuments() {
        return documents.stream().filter(DocumentOutcome::isRead).toList();
    }
}
