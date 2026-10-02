package io.agenticai.reg.service;

import io.agenticai.reg.domain.ConnectionType;
import io.agenticai.reg.domain.FetchMode;
import io.agenticai.reg.domain.ParsedServiceDefinition;
import io.agenticai.reg.domain.ParsedServiceDefinition.ParsedDocuments;
import io.agenticai.reg.domain.ParsedServiceDefinition.ParsedQuery;
import io.agenticai.reg.domain.ServiceCodes;
import io.agenticai.reg.domain.ServiceDefinitionParseResult;
import io.agenticai.reg.domain.ServiceDefinitionParser;
import io.agenticai.reg.entity.Connection;
import io.agenticai.reg.port.PackageFolder;
import io.agenticai.reg.repository.ConnectionRepository;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static io.agenticai.reg.service.LoadReasonCodes.APPROVAL_API_UNDEFINED;
import static io.agenticai.reg.service.LoadReasonCodes.BLOB_NOT_JDBC;
import static io.agenticai.reg.service.LoadReasonCodes.CONNECTION_NOT_ACTIVATED;
import static io.agenticai.reg.service.LoadReasonCodes.DOCUMENT_SOURCE_INCOMPLETE;
import static io.agenticai.reg.service.LoadReasonCodes.DUPLICATE_DOCUMENT_TYPE;
import static io.agenticai.reg.service.LoadReasonCodes.DUPLICATE_QUERY_NAME;
import static io.agenticai.reg.service.LoadReasonCodes.ELEMENT_NOT_ALLOWED;
import static io.agenticai.reg.service.LoadReasonCodes.EMPTY_SERVICE_KNOWLEDGE;
import static io.agenticai.reg.service.LoadReasonCodes.FOREIGN_FILE_IN_PACKAGE;
import static io.agenticai.reg.service.LoadReasonCodes.INCOMPLETE_PACKAGE;
import static io.agenticai.reg.service.LoadReasonCodes.INVALID_SERVICE_CODE;
import static io.agenticai.reg.service.LoadReasonCodes.NOT_SINGLE_SELECT;
import static io.agenticai.reg.service.LoadReasonCodes.PACKAGE_CHANGED_DURING_READ;
import static io.agenticai.reg.service.LoadReasonCodes.PACKAGE_FILE_UNREADABLE;
import static io.agenticai.reg.service.LoadReasonCodes.UNBOUND_PARAMETER;
import static io.agenticai.reg.service.LoadReasonCodes.UNKNOWN_FETCH_MODE;

/**
 * Load run step 3, per folder (REQ-REG-005 … REQ-REG-007): validates one package folder in the
 * order the plan fixes — RULE-REG-026, 024, 020, 001, 018, 012, 022, 027, 008, 019, 021, 006,
 * 007, 005, 009, 010, 011 — and answers the first failed rule as the folder's load reason.
 * RULE-REG-002 across folders is {@link DuplicateServiceCodes}, applied afterwards. Reads
 * {@code REG_CONNECTION} for RULE-REG-005 and RULE-REG-010 (after step 2 activated this
 * environment's connections); writes nothing.
 *
 * <p>Interim treatment, recorded as an {@code api_doc_gaps} row of this unit: the catalog names
 * no load reason for a definition that is not parseable YAML, nor for one lacking a required
 * element ({@code version}, {@code input}, a query's {@code sql} or {@code connection}) or
 * declaring {@code version} below 1. Both are rejected under RULE-REG-012 with the element given
 * as {@code malformed: <detail>}, {@code missing: <path>} or {@code invalid: <path>}. A query
 * name declared twice, which the structured parser refuses as a duplicate key, is RULE-REG-019.
 *
 * <p>The service code a message names is the declared code in canonical form; when the folder
 * declared none the folder name stands in, so every message names its subject.
 */
@Component
class PackageValidator {

    private static final Pattern CANONICAL = Pattern.compile(ServiceCodes.CANONICAL_PATTERN);

    private final ConnectionRepository connections;
    private final ServiceDefinitionParser parser = new ServiceDefinitionParser();

    PackageValidator(ConnectionRepository connections) {
        this.connections = Objects.requireNonNull(connections, "connections");
    }

    FolderVerdict validate(PackageFolder folder) {
        Objects.requireNonNull(folder, "folder");
        String folderName = folder.folderName();
        DefinitionScan scan = DefinitionScan.of(folder.definitionText());
        String canonical = scan.declaredServiceCode().map(ServiceCodes::canonical).orElse(null);
        String subject = canonical == null || canonical.isEmpty() ? folderName : canonical;
        FolderVerdict verdict = new FolderVerdict(folderName, canonical, scan.declaredVersion().orElse(null),
                folder.knowledgeText(), folder.definitionText(), null, null);

        // RULE-REG-026 — every file readable (REQ-REG-071)
        if (folder.unreadableFile() != null) {
            return verdict.rejectedBy(LoadReason.of(PACKAGE_FILE_UNREADABLE, folder.unreadableFile(), folderName));
        }
        // RULE-REG-024 — stable read (REQ-REG-069)
        if (!folder.stable()) {
            return verdict.rejectedBy(LoadReason.of(PACKAGE_CHANGED_DURING_READ, folderName));
        }
        // RULE-REG-020 — only the two files (REQ-REG-062)
        if (!folder.otherFileNames().isEmpty()) {
            return verdict.rejectedBy(LoadReason.of(FOREIGN_FILE_IN_PACKAGE, folderName, folder.otherFileNames().getFirst()));
        }
        // RULE-REG-001 — both files (REQ-REG-003)
        String knowledge = folder.knowledgeText();
        String definitionText = folder.definitionText();
        if (knowledge == null || definitionText == null) {
            return verdict.rejectedBy(LoadReason.of(INCOMPLETE_PACKAGE, folderName));
        }
        // RULE-REG-018 — knowledge not empty (REQ-REG-028)
        if (knowledge.isBlank()) {
            return verdict.rejectedBy(LoadReason.of(EMPTY_SERVICE_KNOWLEDGE, subject));
        }
        // RULE-REG-012 — closed structure (REQ-REG-034, REQ-REG-041)
        ParsedServiceDefinition definition;
        switch (parser.parse(definitionText)) {
            case ServiceDefinitionParseResult.Parsed parsed -> definition = parsed.definition();
            case ServiceDefinitionParseResult.ElementNotAllowed notAllowed -> {
                return verdict.rejectedBy(LoadReason.of(ELEMENT_NOT_ALLOWED, subject, notAllowed.element()));
            }
            case ServiceDefinitionParseResult.Malformed malformed -> {
                Optional<String> duplicateQuery = scan.firstDuplicateQueryName();
                if (duplicateQuery.isPresent()) {
                    // RULE-REG-019 — the parser refuses the duplicate key; the rule names the query
                    return verdict.rejectedBy(LoadReason.of(DUPLICATE_QUERY_NAME, duplicateQuery.get(), subject));
                }
                return verdict.rejectedBy(LoadReason.of(ELEMENT_NOT_ALLOWED, subject, "malformed: " + malformed.detail()));
            }
        }
        verdict = verdict.withDefinition(definition);
        Optional<String> missing = firstMissingElement(definition);
        if (missing.isPresent()) {
            return verdict.rejectedBy(LoadReason.of(ELEMENT_NOT_ALLOWED, subject, missing.get()));
        }
        // RULE-REG-022 — valid service code after canonicalisation (REQ-REG-065, ADR-REG-017)
        if (canonical == null || canonical.isEmpty()
                || ValueLengths.length(canonical) > ServiceCodes.MAX_LENGTH
                || !CANONICAL.matcher(canonical).matches()) {
            return verdict.rejectedBy(LoadReason.of(INVALID_SERVICE_CODE, canonical == null ? "" : canonical));
        }
        String code = canonical;
        // RULE-REG-027 — folder name and every bounded definition value within its column (REQ-REG-073)
        Optional<LoadReason> tooLong = firstTooLong(folderName, code, definition);
        if (tooLong.isPresent()) {
            return verdict.rejectedBy(tooLong.get());
        }
        // RULE-REG-008 — fetch mode closed (REQ-REG-038)
        ParsedDocuments documents = definition.documents();
        String fetchText = documents == null ? null : documents.fetchMode();
        FetchMode fetchMode = fetchText == null ? null : FetchMode.fromStored(fetchText).orElse(null);
        if (fetchMode == null) {
            return verdict.rejectedBy(LoadReason.of(UNKNOWN_FETCH_MODE, fetchText));
        }
        // RULE-REG-019 — unique query names (REQ-REG-035)
        Set<String> queryNames = new HashSet<>();
        for (ParsedQuery query : definition.queries()) {
            if (!queryNames.add(query.queryName())) {
                return verdict.rejectedBy(LoadReason.of(DUPLICATE_QUERY_NAME, query.queryName(), code));
            }
        }
        // RULE-REG-021 — unique required document types (REQ-REG-063)
        Set<String> documentTypes = new HashSet<>();
        for (String type : documents.requiredDocumentTypes()) {
            if (!documentTypes.add(type)) {
                return verdict.rejectedBy(LoadReason.of(DUPLICATE_DOCUMENT_TYPE, type, code));
            }
        }
        // RULE-REG-006 — bound parameters only (REQ-REG-032)
        for (ParsedQuery query : definition.queries()) {
            Optional<String> marker = SqlTextRules.firstUnboundMarker(query.sqlText(), definition.inputName());
            if (marker.isPresent()) {
                return verdict.rejectedBy(LoadReason.of(UNBOUND_PARAMETER, query.queryName(), marker.get(), definition.inputName()));
            }
        }
        // RULE-REG-007 — single SELECT statement (REQ-REG-033)
        for (ParsedQuery query : definition.queries()) {
            if (!SqlTextRules.isSingleSelect(query.sqlText())) {
                return verdict.rejectedBy(LoadReason.of(NOT_SINGLE_SELECT, query.queryName()));
            }
        }
        // RULE-REG-005 — every query's connection registered (REQ-REG-031)
        for (ParsedQuery query : definition.queries()) {
            if (!connections.existsByConnectionName(query.connectionName())) {
                return verdict.rejectedBy(LoadReason.of(CONNECTION_NOT_ACTIVATED, query.queryName(), query.connectionName()));
            }
        }
        // RULE-REG-009 — document source complete for path / blob (REQ-REG-039)
        if (fetchMode != FetchMode.MANUAL) {
            String locationColumn = fetchMode == FetchMode.PATH ? documents.pathColumn() : documents.contentColumn();
            if (isBlank(documents.sourceQueryName()) || isBlank(documents.typeColumn()) || isBlank(locationColumn)
                    || !queryNames.contains(documents.sourceQueryName())) {
                return verdict.rejectedBy(LoadReason.of(DOCUMENT_SOURCE_INCOMPLETE, code));
            }
        }
        // RULE-REG-010 — blob over jdbc only (REQ-REG-040)
        if (fetchMode == FetchMode.BLOB) {
            String connectionName = sourceQuery(definition, documents.sourceQueryName()).connectionName();
            boolean jdbc = connections.findByConnectionName(connectionName)
                    .map(Connection::getConnectionType)
                    .filter(type -> type == ConnectionType.JDBC)
                    .isPresent();
            if (!jdbc) {
                return verdict.rejectedBy(LoadReason.of(BLOB_NOT_JDBC, connectionName));
            }
        }
        // RULE-REG-011 — approval definition required when enabled (REQ-REG-043)
        if (definition.approval() != null && Boolean.TRUE.equals(definition.approval().enabled())
                && isBlank(definition.approval().api())) {
            return verdict.rejectedBy(LoadReason.of(APPROVAL_API_UNDEFINED, code));
        }
        return verdict;
    }

    /** The interim "required element" check (see the class comment). */
    private static Optional<String> firstMissingElement(ParsedServiceDefinition definition) {
        if (definition.versionNumber() == null) {
            return Optional.of("missing: " + ServiceDefinitionParser.VERSION_KEY);
        }
        if (definition.versionNumber() < 1) {
            return Optional.of("invalid: " + ServiceDefinitionParser.VERSION_KEY);
        }
        if (isBlank(definition.inputName())) {
            return Optional.of("missing: " + ServiceDefinitionParser.INPUT_KEY);
        }
        for (ParsedQuery query : definition.queries()) {
            String path = ServiceDefinitionParser.QUERIES_KEY + "." + query.queryName() + ".";
            if (query.sqlText() == null) {
                return Optional.of("missing: " + path + ServiceDefinitionParser.QUERY_SQL_KEY);
            }
            if (query.connectionName() == null) {
                return Optional.of("missing: " + path + ServiceDefinitionParser.QUERY_CONNECTION_KEY);
            }
        }
        return Optional.empty();
    }

    /** RULE-REG-027 over the folder name and every bounded value of the definition, in declaration order. */
    private static Optional<LoadReason> firstTooLong(String folderName, String code, ParsedServiceDefinition definition) {
        Optional<LoadReason> reason = ValueLengths.tooLong("folder name", folderName, folderName, ValueLengths.FOLDER_NAME);
        if (reason.isPresent()) {
            return reason;
        }
        reason = ValueLengths.tooLong("input name", code, definition.inputName(), ValueLengths.INPUT_NAME);
        if (reason.isPresent()) {
            return reason;
        }
        for (ParsedQuery query : definition.queries()) {
            reason = ValueLengths.tooLong("query name", code, query.queryName(), ValueLengths.QUERY_NAME);
            if (reason.isPresent()) {
                return reason;
            }
            reason = ValueLengths.tooLong("connection name", code, query.connectionName(), ValueLengths.QUERY_CONNECTION_NAME);
            if (reason.isPresent()) {
                return reason;
            }
        }
        ParsedDocuments documents = definition.documents();
        if (documents != null) {
            reason = ValueLengths.tooLong("document source query", code, documents.sourceQueryName(), ValueLengths.DOCUMENT_SOURCE_QUERY)
                    .or(() -> ValueLengths.tooLong("document type column", code, documents.typeColumn(), ValueLengths.DOCUMENT_COLUMN))
                    .or(() -> ValueLengths.tooLong("document path column", code, documents.pathColumn(), ValueLengths.DOCUMENT_COLUMN))
                    .or(() -> ValueLengths.tooLong("document content column", code, documents.contentColumn(), ValueLengths.DOCUMENT_COLUMN));
            if (reason.isPresent()) {
                return reason;
            }
            for (String type : documents.requiredDocumentTypes()) {
                reason = ValueLengths.tooLong("document type", code, type, ValueLengths.DOCUMENT_TYPE);
                if (reason.isPresent()) {
                    return reason;
                }
            }
        }
        if (definition.approval() != null) {
            reason = ValueLengths.tooLong("approval API", code, definition.approval().api(), ValueLengths.APPROVAL_API);
            if (reason.isPresent()) {
                return reason;
            }
        }
        return Optional.empty();
    }

    private static ParsedQuery sourceQuery(ParsedServiceDefinition definition, String sourceQueryName) {
        List<ParsedQuery> queries = definition.queries();
        for (ParsedQuery query : queries) {
            if (query.queryName().equals(sourceQueryName)) {
                return query;
            }
        }
        throw new IllegalStateException("RULE-REG-009 guarantees the document source query is declared");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
