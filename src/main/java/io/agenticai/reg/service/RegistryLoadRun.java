package io.agenticai.reg.service;

import io.agenticai.platform.tx.JdbcSavepoints;
import io.agenticai.reg.config.ServiceRegistryProperties;
import io.agenticai.reg.domain.LoadOutcome;
import io.agenticai.reg.domain.LoadSubject;
import io.agenticai.reg.port.DirectoryStatus;
import io.agenticai.reg.port.PackageFolder;
import io.agenticai.reg.port.PackageSource;
import io.agenticai.reg.repository.LoadResultRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The start-up load run (ADR-REG-007, ADR-REG-011, ADR-REG-015): runs once per start, after the
 * schema is ready and before the service accepts traffic, as ONE read-write transaction under one
 * exclusive database lock. Orchestration only — every rule is decided by a collaborator.
 *
 * <p><b>Lifecycle.</b> A {@link SmartLifecycle} at {@link #PHASE} 0: every bean is wired and the
 * schema validated before the lifecycle starts, and Spring Boot's embedded web server starts in
 * its own {@code SmartLifecycle} at phase {@code Integer.MAX_VALUE - 1}, so {@link #start()}
 * completes before the first request is accepted. A failure of the run fails the start.
 * {@link #run()} is public so a test can start a load run again without restarting the context.
 *
 * <p><b>Steps</b> (SVC-API): 0 take the load lock ({@link LoadLock}; not granted → nothing is
 * written, the registry is served as stored, REQ-REG-068); 1 {@code loadRunAt = now}, delete
 * every earlier Load Result (REQ-REG-009); 2 activate connections ({@link ConnectionActivation});
 * 3 guard the package directory ({@link PackageDirectoryGuard} — unavailable → one
 * {@code PACKAGE_DIRECTORY} row, steps 4–5 skipped) and validate every folder
 * ({@link PackageValidator}, then {@link DuplicateServiceCodes}); 4 register versions
 * ({@link VersionRegistration}); 5 withdraw / restore ({@link ServiceWithdrawal}); 6 the pilot
 * package passes like any other. Every item runs behind its own savepoint
 * ({@link ItemSavepoints}); every Load Result row is written by {@link LoadResultRecorder}.
 */
@Component
public class RegistryLoadRun implements SmartLifecycle {

    /**
     * The lifecycle phase: after bean initialisation, before the web server's phase
     * ({@code Integer.MAX_VALUE - 1}) — the load run completes before traffic is accepted.
     */
    public static final int PHASE = 0;

    private static final Logger log = LoggerFactory.getLogger(RegistryLoadRun.class);

    private final TransactionTemplate transaction;
    private final EntityManager entityManager;
    private final JdbcSavepoints jdbcSavepoints;
    private final ServiceRegistryProperties properties;
    private final PackageSource packageSource;
    private final LoadResultRepository loadResults;
    private final LoadLock loadLock;
    private final LoadResultRecorder recorder;
    private final ConnectionActivation connectionActivation;
    private final PackageDirectoryGuard directoryGuard;
    private final PackageValidator validator;
    private final VersionRegistration registration;
    private final ServiceWithdrawal withdrawal;

    private volatile boolean running;

    RegistryLoadRun(PlatformTransactionManager transactionManager,
                    EntityManager entityManager,
                    JdbcSavepoints jdbcSavepoints,
                    ServiceRegistryProperties properties,
                    PackageSource packageSource,
                    LoadResultRepository loadResults,
                    LoadLock loadLock,
                    LoadResultRecorder recorder,
                    ConnectionActivation connectionActivation,
                    PackageDirectoryGuard directoryGuard,
                    PackageValidator validator,
                    VersionRegistration registration,
                    ServiceWithdrawal withdrawal) {
        TransactionTemplate template = new TransactionTemplate(Objects.requireNonNull(transactionManager, "transactionManager"));
        template.setName("REG load run");
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        template.setReadOnly(false);
        this.transaction = template;
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.jdbcSavepoints = Objects.requireNonNull(jdbcSavepoints, "jdbcSavepoints");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.packageSource = Objects.requireNonNull(packageSource, "packageSource");
        this.loadResults = Objects.requireNonNull(loadResults, "loadResults");
        this.loadLock = Objects.requireNonNull(loadLock, "loadLock");
        this.recorder = Objects.requireNonNull(recorder, "recorder");
        this.connectionActivation = Objects.requireNonNull(connectionActivation, "connectionActivation");
        this.directoryGuard = Objects.requireNonNull(directoryGuard, "directoryGuard");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.registration = Objects.requireNonNull(registration, "registration");
        this.withdrawal = Objects.requireNonNull(withdrawal, "withdrawal");
    }

    /** One complete load run in one transaction. */
    public LoadRunOutcome run() {
        OffsetDateTime loadRunAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);
        LoadRunOutcome outcome = transaction.execute(status -> {
            // step 0 — the lock is the transaction's first statement
            if (!loadLock.acquire()) {
                status.setRollbackOnly();
                return LoadRunOutcome.LOCK_NOT_GRANTED;
            }
            ItemSavepoints savepoints = new ItemSavepoints(jdbcSavepoints);
            LoadRunTally tally = new LoadRunTally();
            // step 1 — start the run
            loadResults.deleteAllInBatch();
            // step 2 — activate connections
            connectionActivation.activate(loadRunAt, savepoints, tally);
            // steps 3–5 — packages
            DirectoryStatus directoryStatus = packageSource.status();
            if (directoryGuard.unavailable(directoryStatus)) {
                String directory = properties.packageDirectory().toString();
                savepoints.run(directory, () -> recorder.record(loadRunAt, LoadSubject.PACKAGE_DIRECTORY, directory,
                        null, null, LoadOutcome.REJECTED,
                        LoadReason.of(LoadReasonCodes.PACKAGE_DIRECTORY_UNAVAILABLE, directory)));
                log.warn("REG load run: package directory {} ({}); no package loaded, no service withdrawn",
                        directoryStatus, directory);
            } else {
                loadPackages(loadRunAt, savepoints, tally);
            }
            entityManager.flush();
            log.info("REG load run completed at {}: {}", loadRunAt, tally.summary());
            return LoadRunOutcome.COMPLETED;
        });
        return outcome == null ? LoadRunOutcome.COMPLETED : outcome;
    }

    private void loadPackages(OffsetDateTime loadRunAt, ItemSavepoints savepoints, LoadRunTally tally) {
        // REQ-REG-016 — an entry refused because its real path lies outside the directory is never read;
        // no Load Result row is prescribed (TC-REG-017), so it is said here, never silently (G.6)
        for (String refused : packageSource.refusedEntries()) {
            log.warn("REG load run: package directory entry \"{}\" refused — its real path lies outside {}",
                    refused, properties.packageDirectory());
        }
        // step 3 — validate every folder, then RULE-REG-002 across folders
        List<FolderVerdict> verdicts = new ArrayList<>();
        for (PackageFolder folder : packageSource.folders()) {
            savepoints.run(folder.folderName(), () -> verdicts.add(validator.validate(folder)));
        }
        List<FolderVerdict> judged = DuplicateServiceCodes.apply(verdicts);
        // step 4 — register versions; one Load Result row per folder
        Set<String> declaredCodes = new HashSet<>();
        Set<String> acceptedCodes = new HashSet<>();
        for (FolderVerdict verdict : judged) {
            if (verdict.canonicalCode() != null && !verdict.canonicalCode().isEmpty()) {
                declaredCodes.add(verdict.canonicalCode());
            }
            savepoints.run(verdict.folderName(), () -> {
                if (verdict.rejected()) {
                    recorder.record(loadRunAt, LoadSubject.SERVICE_PACKAGE, verdict.folderName(), verdict.canonicalCode(),
                            verdict.declaredVersion(), LoadOutcome.REJECTED, verdict.rejection());
                    tally.packagesRejected++;
                } else if (registration.register(loadRunAt, verdict, tally)) {
                    acceptedCodes.add(verdict.canonicalCode());
                }
            });
        }
        // step 5 — withdraw / restore
        withdrawal.apply(loadRunAt, declaredCodes, acceptedCodes, savepoints, tally);
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        LoadRunOutcome outcome = run();
        running = true;
        if (outcome == LoadRunOutcome.LOCK_NOT_GRANTED) {
            log.warn("REG start-up load run skipped: serving the registry as stored");
        }
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return PHASE;
    }
}
