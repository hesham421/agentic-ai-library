package io.agenticai.reg.repository;

/**
 * A stored version with fetch mode {@code blob} whose document source query runs on a given
 * connection — the business key of ENT-REG-002 as RULE-REG-025 names it in its message
 * (ADR-REG-021). Projection of {@link ServicePackageVersionRepository#findBlobVersionsReadingThrough}.
 *
 * @param serviceCode   the canonical service code of the version's package
 * @param versionNumber the version number
 */
public record BlobVersionReference(String serviceCode, Integer versionNumber) {
}
