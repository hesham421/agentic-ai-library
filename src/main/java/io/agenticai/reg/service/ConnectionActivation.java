package io.agenticai.reg.service;

import io.agenticai.reg.domain.ConnectionType;
import io.agenticai.reg.domain.FetchMode;
import io.agenticai.reg.domain.LoadOutcome;
import io.agenticai.reg.domain.LoadSubject;
import io.agenticai.reg.entity.Connection;
import io.agenticai.reg.port.Activation;
import io.agenticai.reg.port.Activation.ConnectionEntry;
import io.agenticai.reg.port.ActivationSource;
import io.agenticai.reg.repository.BlobVersionReference;
import io.agenticai.reg.repository.ConnectionRepository;
import io.agenticai.reg.repository.ServicePackageVersionRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static io.agenticai.reg.service.LoadReasonCodes.CONNECTION_NOT_READ_ONLY;
import static io.agenticai.reg.service.LoadReasonCodes.CONNECTION_TYPE_BREAKS_BLOB;
import static io.agenticai.reg.service.LoadReasonCodes.DUPLICATE_CONNECTION_NAME;
import static io.agenticai.reg.service.LoadReasonCodes.UNKNOWN_CONNECTION_TYPE;

/**
 * Load run step 2 — connection activation (REQ-REG-049 … REQ-REG-056, ADR-REG-009). Reads the
 * {@link ActivationSource} and, per entry in declared order and behind its own savepoint, refuses
 * it in the plan's order — RULE-REG-013 (every entry of a duplicated name), RULE-REG-014,
 * RULE-REG-015, RULE-REG-027 (an over-length environment name refuses every entry), RULE-REG-025
 * (ADR-REG-021: a stored row keeps every previous setting, a removed name is not re-registered) —
 * or registers it: insert ({@code ACTIVATED}), update when any setting changed ({@code UPDATED},
 * REQ-REG-050; an unchanged entry is recorded {@code ACTIVATED} and left untouched), and finally
 * deletes every registered connection the configuration no longer lists ({@code REMOVED},
 * REQ-REG-051). A refused entry leaves an existing row of its name unchanged and is never
 * removed. Only the credential reference is stored (REQ-REG-054). One Load Result row per
 * outcome, subject kind CONNECTION; the run continues after every refusal (REQ-REG-007).
 *
 * <p>Interim (recorded as an {@code api_doc_gaps} row of this unit): the catalog names no load
 * reason for an entry missing its name, endpoint, dialect or credential reference, for an
 * {@code mcp} entry without a query tool, or for a blank {@code aias.registry.environment-name}
 * while entries are listed. Such an entry is refused like any other failed validation — one
 * REJECTED row, behind its savepoint, the run continues (REQ-REG-007, ADR-REG-015) — with a plain
 * English reason and no code; a blank environment name refuses every entry, by analogy with
 * ADR-REG-022's over-length environment name. A missing value is detected where it is first
 * needed: a blank name (which cannot be deduplicated) before RULE-REG-013, the environment name
 * right after it, the other values after RULE-REG-014 and before RULE-REG-015 / RULE-REG-027.
 * An entry without a name is reported under its property path {@code connections[<index>]}.
 */
@Component
class ConnectionActivation {

    // interim: no load reason code stated — api_doc_gaps row open
    static final String MISSING_VALUE_REASON =
            "The connection \"%s\" lacks its %s; name, type, endpoint, dialect and credential reference are required, and a query tool for mcp.";
    // interim: no load reason code stated — api_doc_gaps row open
    static final String MISSING_ENVIRONMENT_REASON =
            "The environment name is missing; no connection was activated.";

    private final ActivationSource activationSource;
    private final ConnectionRepository connections;
    private final ServicePackageVersionRepository versions;
    private final LoadResultRecorder recorder;

    ConnectionActivation(ActivationSource activationSource,
                         ConnectionRepository connections,
                         ServicePackageVersionRepository versions,
                         LoadResultRecorder recorder) {
        this.activationSource = Objects.requireNonNull(activationSource, "activationSource");
        this.connections = Objects.requireNonNull(connections, "connections");
        this.versions = Objects.requireNonNull(versions, "versions");
        this.recorder = Objects.requireNonNull(recorder, "recorder");
    }

    void activate(OffsetDateTime loadRunAt, ItemSavepoints savepoints, LoadRunTally tally) {
        Activation activation = activationSource.activation();
        List<ConnectionEntry> entries = activation.connections();
        String environmentName = activation.environmentName();
        boolean environmentMissing = environmentName == null || environmentName.isBlank();
        Map<String, Integer> listed = new HashMap<>();
        for (ConnectionEntry entry : entries) {
            if (!isBlank(entry.name())) {
                listed.merge(entry.name(), 1, Integer::sum);
            }
        }
        Optional<LoadReason> environmentTooLong = environmentMissing ? Optional.empty() : ValueLengths.tooLong(
                "environment name", environmentName, environmentName, ValueLengths.ENVIRONMENT_NAME);

        for (int index = 0; index < entries.size(); index++) {
            ConnectionEntry entry = entries.get(index);
            String subject = isBlank(entry.name()) ? "connections[" + index + "]" : entry.name();
            savepoints.run(subject, () -> activateOne(loadRunAt, environmentName, environmentMissing, entry, subject,
                    listed.getOrDefault(subject, 0) > 1, environmentTooLong, tally));
        }

        Set<String> listedNames = new HashSet<>(listed.keySet());
        for (Connection registered : connections.findAll()) {
            String name = registered.getConnectionName();
            if (!listedNames.contains(name)) {
                savepoints.run(name, () -> {
                    connections.delete(registered);
                    recorder.record(loadRunAt, LoadSubject.CONNECTION, name, null, null, LoadOutcome.REMOVED, null);
                    tally.removed++;
                });
            }
        }
    }

    private void activateOne(OffsetDateTime loadRunAt,
                             String environmentName,
                             boolean environmentMissing,
                             ConnectionEntry entry,
                             String subject,
                             boolean duplicated,
                             Optional<LoadReason> environmentTooLong,
                             LoadRunTally tally) {
        // interim refusals (no code) — see the class comment
        Optional<String> interim = interimRefusal(environmentMissing, entry, subject);
        if (interim.isPresent() && isBlank(entry.name())) {
            recorder.recordInterim(loadRunAt, LoadSubject.CONNECTION, subject, LoadOutcome.REJECTED, interim.get());
            tally.connectionsRefused++;
            return;
        }
        Optional<LoadReason> refusal = refusal(entry, subject, duplicated, environmentTooLong, interim);
        if (refusal.isPresent()) {
            recorder.record(loadRunAt, LoadSubject.CONNECTION, subject, null, null, LoadOutcome.REJECTED, refusal.get());
            tally.connectionsRefused++;
            return;
        }
        if (interim.isPresent()) {
            recorder.recordInterim(loadRunAt, LoadSubject.CONNECTION, subject, LoadOutcome.REJECTED, interim.get());
            tally.connectionsRefused++;
            return;
        }
        ConnectionType type = ConnectionType.fromStored(entry.type()).orElseThrow();
        boolean limitedToViews = Boolean.TRUE.equals(entry.limitedToViews());

        Optional<Connection> existing = connections.findByConnectionName(subject);
        if (existing.isEmpty()) {
            connections.save(Connection.fromActivation(subject, type, entry.endpoint(), entry.queryTool(), entry.dialect(),
                    entry.credentialReference(), true, limitedToViews, environmentName, loadRunAt));
            recorder.record(loadRunAt, LoadSubject.CONNECTION, subject, null, null, LoadOutcome.ACTIVATED, null);
            tally.activated++;
            return;
        }
        Connection connection = existing.get();
        if (changed(connection, type, entry, limitedToViews, environmentName)) {
            connection.updateFromActivation(type, entry.endpoint(), entry.queryTool(), entry.dialect(),
                    entry.credentialReference(), true, limitedToViews, environmentName, loadRunAt);
            connections.save(connection);
            recorder.record(loadRunAt, LoadSubject.CONNECTION, subject, null, null, LoadOutcome.UPDATED, null);
            tally.updated++;
        } else {
            recorder.record(loadRunAt, LoadSubject.CONNECTION, subject, null, null, LoadOutcome.ACTIVATED, null);
            tally.activated++;
        }
    }

    /**
     * The first activation rule the entry fails, in the plan's order — RULE-REG-013, RULE-REG-014,
     * (interim missing-value refusal), RULE-REG-015, RULE-REG-027, RULE-REG-025; empty when the
     * entry may be registered. {@code interim} is consulted between RULE-REG-014 and RULE-REG-015
     * so a missing value is reported before any later rule on that value.
     */
    private Optional<LoadReason> refusal(ConnectionEntry entry,
                                         String name,
                                         boolean duplicated,
                                         Optional<LoadReason> environmentTooLong,
                                         Optional<String> interim) {
        // RULE-REG-013 — unique connection name (REQ-REG-047)
        if (duplicated) {
            return Optional.of(LoadReason.of(DUPLICATE_CONNECTION_NAME, name));
        }
        // RULE-REG-014 — connection type closed (REQ-REG-052)
        Optional<ConnectionType> type = ConnectionType.fromStored(entry.type());
        if (type.isEmpty()) {
            return Optional.of(LoadReason.of(UNKNOWN_CONNECTION_TYPE, name, entry.type()));
        }
        if (interim.isPresent()) {
            return Optional.empty(); // the caller records the interim refusal
        }
        // RULE-REG-015 — read-only declaration required (REQ-REG-055)
        if (!Boolean.TRUE.equals(entry.readOnly())) {
            return Optional.of(LoadReason.of(CONNECTION_NOT_READ_ONLY, name));
        }
        // RULE-REG-027 — every text value within its column; the environment name refuses every entry (REQ-REG-073)
        if (environmentTooLong.isPresent()) {
            return environmentTooLong;
        }
        Optional<LoadReason> tooLong = ValueLengths.tooLong("connection name", name, name, ValueLengths.CONNECTION_NAME)
                .or(() -> ValueLengths.tooLong("endpoint", name, entry.endpoint(), ValueLengths.ENDPOINT))
                .or(() -> ValueLengths.tooLong("query tool", name, entry.queryTool(), ValueLengths.QUERY_TOOL))
                .or(() -> ValueLengths.tooLong("dialect", name, entry.dialect(), ValueLengths.DIALECT))
                .or(() -> ValueLengths.tooLong("credential reference", name, entry.credentialReference(), ValueLengths.CREDENTIAL_REFERENCE));
        if (tooLong.isPresent()) {
            return tooLong;
        }
        // RULE-REG-025 — a connection a stored blob version reads through stays jdbc (REQ-REG-072, ADR-REG-021)
        if (type.get() != ConnectionType.JDBC) {
            List<BlobVersionReference> blobVersions = versions.findBlobVersionsReadingThrough(name, FetchMode.BLOB);
            if (!blobVersions.isEmpty()) {
                BlobVersionReference first = blobVersions.getFirst();
                return Optional.of(LoadReason.of(CONNECTION_TYPE_BREAKS_BLOB, name, first.versionNumber(), first.serviceCode()));
            }
        }
        return Optional.empty();
    }

    /** The interim refusal text of a missing required value, or empty (see the class comment). */
    private static Optional<String> interimRefusal(boolean environmentMissing, ConnectionEntry entry, String subject) {
        if (isBlank(entry.name())) {
            return Optional.of(MISSING_VALUE_REASON.formatted(subject, "name"));
        }
        if (environmentMissing) {
            return Optional.of(MISSING_ENVIRONMENT_REASON);
        }
        if (isBlank(entry.endpoint())) {
            return Optional.of(MISSING_VALUE_REASON.formatted(subject, "endpoint"));
        }
        if (isBlank(entry.dialect())) {
            return Optional.of(MISSING_VALUE_REASON.formatted(subject, "dialect"));
        }
        if (isBlank(entry.credentialReference())) {
            return Optional.of(MISSING_VALUE_REASON.formatted(subject, "credential reference"));
        }
        if (ConnectionType.fromStored(entry.type()).filter(t -> t == ConnectionType.MCP).isPresent() && isBlank(entry.queryTool())) {
            return Optional.of(MISSING_VALUE_REASON.formatted(subject, "query tool"));
        }
        return Optional.empty();
    }

    /** REQ-REG-050 — whether any stored setting differs from the declared one (every field compared). */
    private static boolean changed(Connection stored,
                                   ConnectionType type,
                                   ConnectionEntry entry,
                                   boolean limitedToViews,
                                   String environmentName) {
        return stored.getConnectionType() != type
                || !entry.endpoint().equals(stored.getEndpoint())
                || !Objects.equals(entry.queryTool(), stored.getQueryTool())
                || !entry.dialect().equals(stored.getDialect())
                || !entry.credentialReference().equals(stored.getCredentialReference())
                || !stored.isReadOnly()
                || limitedToViews != stored.isLimitedToViews()
                || !environmentName.equals(stored.getEnvironmentName());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
