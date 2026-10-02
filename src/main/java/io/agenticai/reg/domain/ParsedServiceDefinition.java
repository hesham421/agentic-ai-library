package io.agenticai.reg.domain;

import java.util.List;

/**
 * A service definition as declared in its YAML, parsed into the closed structure of RULE-REG-012
 * ({@code service, version, input, queries, documents, approval}) and nothing more. Values are
 * exactly as declared: the service code is not canonicalised (the load run does that,
 * REQ-REG-064), the fetch mode is text (RULE-REG-008 is the load run's), and an absent section
 * is {@code null} — its absence is judged by the load run (RULE-REG-009, RULE-REG-011, …).
 *
 * @param serviceCode   the declared {@code service}, or {@code null} when absent
 * @param versionNumber the declared {@code version}, or {@code null} when absent
 * @param inputName     the declared {@code input}, or {@code null} when absent
 * @param queries       the declared queries in declaration order; empty when absent; unmodifiable
 * @param documents     the declared {@code documents} section, or {@code null} when absent
 * @param approval      the declared {@code approval} section, or {@code null} when absent
 */
public record ParsedServiceDefinition(
        String serviceCode,
        Integer versionNumber,
        String inputName,
        List<ParsedQuery> queries,
        ParsedDocuments documents,
        ParsedApproval approval) {

    public ParsedServiceDefinition {
        queries = queries == null ? List.of() : List.copyOf(queries);
    }

    /**
     * One entry of {@code queries} (ENT-REG-003 as declared).
     *
     * @param queryName      the declared query key
     * @param connectionName the declared connection name, or {@code null} when absent
     * @param sqlText        the declared {@code sql}, unaltered, or {@code null} when absent
     */
    public record ParsedQuery(String queryName, String connectionName, String sqlText) {
    }

    /**
     * The {@code documents} section (ENT-REG-002 document fields as declared).
     *
     * @param fetchMode             the declared {@code fetch} text, or {@code null} when absent
     * @param sourceQueryName       the declared document source query name, or {@code null}
     * @param typeColumn            the declared {@code type_column}, or {@code null}
     * @param pathColumn            the declared {@code path_column}, or {@code null}
     * @param contentColumn         the declared {@code content_column}, or {@code null}
     * @param requiredDocumentTypes the declared {@code required} list in declared order,
     *                              duplicates kept (RULE-REG-021 is the load run's); empty when
     *                              absent; unmodifiable
     */
    public record ParsedDocuments(
            String fetchMode,
            String sourceQueryName,
            String typeColumn,
            String pathColumn,
            String contentColumn,
            List<String> requiredDocumentTypes) {

        public ParsedDocuments {
            requiredDocumentTypes = requiredDocumentTypes == null
                    ? List.of()
                    : List.copyOf(requiredDocumentTypes);
        }
    }

    /**
     * The {@code approval} section.
     *
     * @param enabled the declared {@code approval.enabled}, or {@code null} when absent
     * @param api     the declared {@code approval.api}, or {@code null} when absent
     */
    public record ParsedApproval(Boolean enabled, String api) {
    }
}
