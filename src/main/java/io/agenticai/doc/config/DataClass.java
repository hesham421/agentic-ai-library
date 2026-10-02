package io.agenticai.doc.config;

/**
 * The data class of the environment's documents, declared by {@code aias.documents.data-class}
 * (ADR-DOC-009): {@link #SYNTHETIC} for test environments that hold only synthetic or anonymised
 * requests, {@link #REAL} otherwise (the default when not declared). While the document-reading
 * model's tier is {@link ModelTier#FREE} and the data class is {@code REAL}, DOC sends no document
 * to the model (REQ-DOC-058).
 *
 * <p>A configuration value only — never stored in a column (ADR-DOC-009), so it carries no stored
 * value; Spring binds it from the property text.
 */
public enum DataClass {

    SYNTHETIC,
    REAL
}
