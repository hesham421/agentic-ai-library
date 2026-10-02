package io.agenticai.reg.service;

import io.agenticai.reg.config.ServiceRegistryProperties;
import io.agenticai.reg.error.ServiceRegistryErrorCodes;
import io.agenticai.reg.error.ServiceRegistryException;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Objects;

/**
 * Step 0 of the load run — the load lock (REQ-REG-067, REQ-REG-068, ADR-REG-015): the
 * transaction's first statement is {@code LOCK TABLE REG_LOAD_RESULT IN EXCLUSIVE MODE WAIT n},
 * held until the run commits or rolls back.
 *
 * <p>Oracle allows no bind variable in {@code WAIT n}, so the one value in the statement is an
 * integer this class derives from {@code aias.registry.load-lock-timeout} — configuration, never
 * free text — clamped to {@code 0 … Integer.MAX_VALUE} seconds; a zero timeout uses {@code NOWAIT}.
 * No other value enters the statement.
 *
 * <p>{@code ORA-30006} (resource busy; acquire with WAIT timeout expired) and {@code ORA-00054}
 * (the NOWAIT form) mean another instance holds the lock: the run is not started and the instance
 * serves the registry as stored. Any other failure is {@code REG-500} with its cause.
 */
@Component
class LoadLock {

    private static final Logger log = LoggerFactory.getLogger(LoadLock.class);

    /** ORA-30006: resource busy; acquire with WAIT timeout expired. */
    static final int ORA_WAIT_TIMEOUT = 30006;
    /** ORA-00054: resource busy and acquire with NOWAIT specified. */
    static final int ORA_NOWAIT_BUSY = 54;

    private static final String LOCK_TABLE = "LOCK TABLE REG_LOAD_RESULT IN EXCLUSIVE MODE";

    private final EntityManager entityManager;
    private final Duration timeout;

    LoadLock(EntityManager entityManager, ServiceRegistryProperties properties) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.timeout = Objects.requireNonNull(properties, "properties").loadLockTimeout();
    }

    /** The statement this instance executes — built once from configuration. */
    String statement() {
        long seconds = Math.max(0L, timeout.toSeconds());
        int waitSeconds = (int) Math.min(Integer.MAX_VALUE, seconds);
        return waitSeconds == 0 ? LOCK_TABLE + " NOWAIT" : LOCK_TABLE + " WAIT " + waitSeconds;
    }

    /**
     * Takes the lock on the current transaction's connection.
     *
     * @return {@code true} when granted; {@code false} when not granted within the timeout
     * @throws ServiceRegistryException {@code REG-500} for any other database failure
     */
    boolean acquire() {
        String sql = statement();
        try {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                }
            });
            log.debug("REG load lock granted");
            return true;
        } catch (RuntimeException e) {
            int oracleError = oracleErrorCode(e);
            if (oracleError == ORA_WAIT_TIMEOUT || oracleError == ORA_NOWAIT_BUSY) {
                log.warn("REG load lock not granted within {} (ORA-{}): no load run; the registry is served as stored",
                        timeout, oracleError);
                return false;
            }
            throw new ServiceRegistryException(ServiceRegistryErrorCodes.UNEXPECTED_FAILURE, e);
        }
    }

    /** The Oracle error code of the first {@link SQLException} in the cause chain, or -1. */
    static int oracleErrorCode(Throwable failure) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql) {
                return sql.getErrorCode();
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return -1;
    }
}
