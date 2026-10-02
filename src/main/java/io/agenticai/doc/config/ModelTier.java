package io.agenticai.doc.config;

/**
 * The tier of the document-reading model, declared by {@code aias.documents.reading-model.tier}
 * (ADR-DOC-009): {@link #FREE} (the default when not declared — a free tier may use submitted
 * data for training) or {@link #APPROVED} (the go-live provider decision, D3). While the tier is
 * {@code FREE} and the environment's data class is {@link DataClass#REAL}, DOC sends no document
 * to the model and reports every document that needs the reading step UNREADABLE /
 * MODEL_NOT_PERMITTED (REQ-DOC-058).
 *
 * <p>A configuration value only — never stored in a column (ADR-DOC-009), so it carries no stored
 * value; Spring binds it from the property text.
 */
public enum ModelTier {

    FREE,
    APPROVED
}
