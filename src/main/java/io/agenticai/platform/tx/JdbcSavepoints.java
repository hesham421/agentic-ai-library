package io.agenticai.platform.tx;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Objects;

/**
 * Runs a unit of work behind a JDBC savepoint taken on the current transaction's own connection.
 * Module-neutral platform piece, used by REG's load run (ADR-REG-015) and CHK's ending
 * (REQ-CHK-048).
 *
 * <p><b>Why not {@code TransactionStatus.createSavepoint()}.</b> Under Spring's
 * {@code JpaTransactionManager} with {@code HibernateJpaDialect} (spring-orm 7.0.x), the
 * transaction object's savepoint manager is set only when the dialect's transaction data
 * implements {@code SavepointManager}; Hibernate's {@code SessionTransactionData} does not, so
 * {@code createSavepoint()} always throws {@code NestedTransactionNotSupportedException},
 * whatever {@code nestedTransactionAllowed} says. This class therefore takes the savepoint itself,
 * on the JDBC connection Hibernate's session holds for the transaction
 * ({@link Session#doReturningWork}).
 *
 * <p><b>Persistence-context handling.</b> A JDBC rollback to a savepoint undoes the SQL but not the
 * persistence context, which would re-issue the item's inserts/updates at the next flush. So
 * (1) the context is flushed <em>before</em> the savepoint, so it delimits exactly this unit;
 * (2) the unit's SQL is flushed <em>inside</em> the savepoint, so a database refusal surfaces while
 * the savepoint is active; (3) after a rollback the context is <em>cleared</em>, detaching every
 * managed entity (callers must hold no entity across a failed unit — re-read what is needed).
 *
 * <p><b>Release on Oracle.</b> The Oracle JDBC driver does not support
 * {@link java.sql.Connection#releaseSavepoint}; it throws {@code SQLFeatureNotSupportedException}
 * (or a plain {@code SQLException}, depending on the driver version). An unreleased Oracle
 * savepoint is harmless — it is discarded at commit or rollback — so a failed release is logged at
 * DEBUG and ignored.
 *
 * <p><b>Known limit — the outer transaction may still be doomed.</b> Hibernate marks the session's
 * transaction rollback-only on many flush {@code PersistenceException}s, and a Spring
 * {@code @Transactional} participant that throws (e.g. one with {@code rollbackFor}) marks the
 * shared transaction rollback-only too. Rolling back to the JDBC savepoint does not clear either
 * mark, so after such a failure the outer transaction cannot commit even though the savepoint
 * rolled the SQL back: its commit ends in {@code UnexpectedRollbackException}. Callers must treat
 * that as fatal for the transaction — REG aborts the load run with REG-500 anyway; CHK's pipeline
 * falls back to a fresh {@code REQUIRES_NEW} ending (RPT audit X1).
 */
@Component
public class JdbcSavepoints {

    private static final Logger log = LoggerFactory.getLogger(JdbcSavepoints.class);

    private final EntityManager entityManager;

    public JdbcSavepoints(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    /**
     * Runs {@code work} behind its own JDBC savepoint: flush, savepoint, work, flush, release. On a
     * failure of the work or of its flush the connection is rolled back to the savepoint, the
     * persistence context is cleared, and the original exception is rethrown (a failure of the
     * rollback itself is attached to it as suppressed).
     *
     * @param work the unit of work; must not hold managed entities beyond its own scope
     * @throws IllegalStateException when no actual transaction is active
     */
    public void run(Runnable work) {
        Objects.requireNonNull(work, "work");
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("JdbcSavepoints.run requires an active transaction");
        }
        Session session = entityManager.unwrap(Session.class);
        entityManager.flush();
        Savepoint savepoint = session.doReturningWork(connection -> connection.setSavepoint());
        try {
            work.run();
            entityManager.flush();
        } catch (RuntimeException | Error failure) {
            rollbackTo(session, savepoint, failure);
            throw failure;
        }
        release(session, savepoint);
    }

    private void rollbackTo(Session session, Savepoint savepoint, Throwable failure) {
        try {
            session.doWork(connection -> connection.rollback(savepoint));
        } catch (RuntimeException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        } finally {
            entityManager.clear();
        }
    }

    private static void release(Session session, Savepoint savepoint) {
        session.doWork(connection -> {
            try {
                connection.releaseSavepoint(savepoint);
            } catch (SQLException unsupported) {
                // Oracle: release is unsupported (SQLFeatureNotSupportedException is an SQLException);
                // the savepoint is discarded at commit or rollback — harmless to leave it.
                log.debug("JDBC savepoint release not supported by the driver; ignored: {}",
                        unsupported.getMessage());
            }
        });
    }
}
