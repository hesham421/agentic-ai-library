package io.agenticai.chk.domain;

/**
 * An explicit value or date condition as the comparison model stated it (REQ-CHK-024,
 * ADR-CHK-003): the value found in the Check's data, the comparison and the limit. Each part is
 * verified in code before the comparison is recomputed ({@link FindingVerifier}).
 *
 * @param valueFound the value found, as the model copied it; may be {@code null}
 * @param comparison the comparison text; may be {@code null}
 * @param limit      the limit, as the model copied it from the service knowledge; may be {@code null}
 */
public record ExplicitCondition(String valueFound, String comparison, String limit) {
}
