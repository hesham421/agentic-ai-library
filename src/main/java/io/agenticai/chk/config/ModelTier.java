package io.agenticai.chk.config;

/**
 * The tier of the comparison model, declared by {@code aias.check.comparison-model.tier}
 * (ADR-CHK-010): {@link #FREE} (the default when not declared — REQ-CHK-075; a free tier may use
 * submitted data for training) or {@link #APPROVED} (the go-live provider decision, D3). While the
 * tier is {@code FREE} and the environment's data class is {@link DataClass#REAL}, CHK sends
 * nothing to the comparison model and the Check ends FAILED / MODEL_NOT_PERMITTED (REQ-CHK-072,
 * ADR-CHK-006).
 *
 * <p>CHK's own enum: the comparison model's tier is CHK's, separate from the document-reading
 * model's tier of ADR-DOC-009, and DOC's {@code doc.config} package is not importable from CHK.
 * A configuration value only — never stored in a column, so it carries no stored value; Spring
 * binds it from the property text.
 */
public enum ModelTier {

    FREE,
    APPROVED
}
