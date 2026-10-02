package io.agenticai.chk.config;

/**
 * The data class of the environment, read from the single platform value
 * {@code aias.documents.data-class} that Document Access also reads (REQ-CHK-074, ADR-CHK-010,
 * ADR-DOC-009): {@link #SYNTHETIC} for test environments that hold only synthetic or anonymised
 * requests, {@link #REAL} otherwise (the default when not declared). While the comparison model's
 * tier is {@link ModelTier#FREE} and the data class is {@code REAL}, CHK sends nothing to the
 * comparison model (REQ-CHK-072).
 *
 * <p>CHK's own copy of the closed values — DOC's enum lives in {@code doc.config}, which CHK may
 * not import. CHK defines no property of its own for the data class; it is read through
 * {@link EnvironmentDataClass}. A configuration value only — never stored in a column.
 */
public enum DataClass {

    SYNTHETIC,
    REAL
}
