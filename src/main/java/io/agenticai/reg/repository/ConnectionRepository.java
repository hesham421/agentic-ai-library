package io.agenticai.reg.repository;

import io.agenticai.reg.entity.Connection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Persistence of ENT-REG-005 Connection ({@code REG_CONNECTION}). Module-internal. Written only by
 * the activation step of the load run (ADR-REG-009): insert, update in place, hard delete through
 * the inherited {@code save} / {@code delete}; {@code findAll} is the activation diff's read.
 */
public interface ConnectionRepository extends JpaRepository<Connection, Long> {

    /**
     * One connection by its name (CON-REG-011 — the in-process supply; RULE-REG-010 and the
     * activation diff in the load run).
     */
    Optional<Connection> findByConnectionName(String connectionName);

    /**
     * Whether a connection of that name is registered — RULE-REG-005 (load run) and RULE-REG-017
     * (in-process supply). An existence check, the row is never loaded.
     */
    boolean existsByConnectionName(String connectionName);
}
