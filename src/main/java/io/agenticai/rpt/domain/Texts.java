package io.agenticai.rpt.domain;

/**
 * The not-blank test the RPT domain rules share (RULE-RPT-001, 004, 007, 008, 010, 013 — "present
 * and not blank"). CLOB values are checked here, in the domain, because the database cannot
 * (ADR-RPT-011). Values are tested, never changed: identifiers are stored exactly as received.
 */
final class Texts {

    private Texts() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    /** {@code true} when the value is absent, empty or only whitespace. */
    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
