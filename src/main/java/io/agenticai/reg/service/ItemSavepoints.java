package io.agenticai.reg.service;

import io.agenticai.platform.tx.JdbcSavepoints;
import io.agenticai.reg.error.ServiceRegistryErrorCodes;
import io.agenticai.reg.error.ServiceRegistryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * The per-item savepoints of one load run (REQ-REG-007, ADR-REG-015): every connection entry
 * and every package folder is processed behind its own JDBC savepoint, taken on the load run
 * transaction's own connection by the platform's {@link JdbcSavepoints} (Spring's
 * {@code TransactionStatus.createSavepoint()} is unsupported under {@code JpaTransactionManager}
 * with Hibernate), so a failing item rolls back to its savepoint only and never undoes another.
 *
 * <p>Hibernate pitfall, handled by {@link JdbcSavepoints}: a savepoint rollback undoes the SQL but
 * leaves the persistence context holding the entities the item inserted or changed, which a later
 * flush would re-issue. Therefore (1) the context is <em>flushed before</em> every savepoint, so
 * the earlier items' SQL is in the database and the savepoint delimits exactly this item; (2) the
 * item's own SQL is flushed <em>inside</em> the savepoint, so a database refusal surfaces while
 * the savepoint is active; (3) after a rollback the context is <em>cleared</em>, detaching the
 * item's stale entities so nothing of it is re-flushed at commit. Collaborators therefore hold no
 * entity across items: each item re-reads what it needs.
 *
 * <p>A failure inside an item is a persistence failure, not a validation failure (every rule is
 * checked before any write): it is rolled back to the savepoint and then raised as
 * {@code REG-500}, so the start fails loudly — the catalog has no load reason for it and nothing
 * is skipped silently (raw idea §12). Aborting the run is also the only safe course: Hibernate may
 * have marked the transaction rollback-only on the failed flush, which the savepoint rollback does
 * not undo (see {@link JdbcSavepoints}).
 */
final class ItemSavepoints {

    private static final Logger log = LoggerFactory.getLogger(ItemSavepoints.class);

    private final JdbcSavepoints savepoints;

    ItemSavepoints(JdbcSavepoints savepoints) {
        this.savepoints = Objects.requireNonNull(savepoints, "savepoints");
    }

    /**
     * Runs {@code work} behind its own savepoint.
     *
     * @param itemName the connection name, folder name or service code — for the log only
     * @param work     the item's validation and writes
     * @throws ServiceRegistryException {@code REG-500} with the cause when the item's writes failed
     */
    void run(String itemName, Runnable work) {
        try {
            savepoints.run(work);
        } catch (RuntimeException e) {
            log.error("REG load run: item \"{}\" failed and was rolled back to its savepoint", itemName, e);
            if (e instanceof ServiceRegistryException registryFailure) {
                throw registryFailure;
            }
            throw new ServiceRegistryException(ServiceRegistryErrorCodes.UNEXPECTED_FAILURE, e);
        }
    }
}
