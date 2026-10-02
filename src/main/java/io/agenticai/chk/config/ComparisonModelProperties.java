package io.agenticai.chk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The comparison model's configuration, bound from {@code aias.check.comparison-model.*}
 * (REQ-CHK-068, ADR-CHK-010). Environment configuration, never part of the deployable: changing
 * these properties and restarting switches the model with no code change (REQ-CHK-067). The
 * document-reading model is a different configuration and a different bean, never injected into
 * CHK.
 *
 * <p>This nested prefix is CHK's own and distinct from the platform per-Check limits of
 * {@code aias.check.*} ({@link io.agenticai.platform.config.CheckLimitsProperties}), which declare
 * no {@code comparisonModel} component — so exactly one class binds each key (CU.6).
 *
 * <p>Nothing is required at binding, so the record is not {@code @Validated}: an absent provider
 * or model is a valid start-up state in which the pipeline ends each Check FAILED /
 * MODEL_UNAVAILABLE when it reaches the comparison step (ADR-CHK-005), rather than blocking the
 * whole service. The comparison {@code ChatModel} bean built from these values is PORTS-MODEL's,
 * not this unit's.
 *
 * @param provider the comparison model's provider; {@code null} when not set
 * @param model    the model identifier, recorded in the report metadata (REQ-CHK-045,
 *                 REQ-CHK-068); {@code null} when not set
 * @param tier     the model's tier (ADR-CHK-010); default {@link ModelTier#FREE} (REQ-CHK-075)
 */
@ConfigurationProperties(prefix = "aias.check.comparison-model")
public record ComparisonModelProperties(
        String provider,
        String model,
        ModelTier tier) {

    /** Default of {@code aias.check.comparison-model.tier} (REQ-CHK-075). */
    public static final ModelTier DEFAULT_TIER = ModelTier.FREE;

    public ComparisonModelProperties {
        tier = tier == null ? DEFAULT_TIER : tier;
    }
}
