package io.agenticai.chk.config;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Objects;

/**
 * CHK's read-only view of the environment data class (REQ-CHK-074, ADR-CHK-010). The one platform
 * value is {@code aias.documents.data-class}, bound by Document Access; CHK must not define a
 * second property, nor a second {@code @ConfigurationProperties} class for the
 * {@code aias.documents} prefix (CU.6), nor import DOC's properties class (module boundary). So
 * the value is read once, at construction, straight from the {@link Environment}, with the same
 * default as DOC's binding ({@code REAL} when not declared).
 *
 * <p>A value that is neither {@code SYNTHETIC} nor {@code REAL} fails start-up with a message
 * naming the property — DOC's binding refuses the same value, so the two can never disagree.
 * Matching is case-insensitive, as Spring's enum binding is.
 */
@Component
public class EnvironmentDataClass {

    /** The platform property both DOC and CHK read (ADR-CHK-010). */
    public static final String PROPERTY = "aias.documents.data-class";

    /** The value taken when the property is not declared (REQ-CHK-074). */
    public static final DataClass DEFAULT = DataClass.REAL;

    private final DataClass dataClass;

    public EnvironmentDataClass(Environment environment) {
        Objects.requireNonNull(environment, "environment");
        this.dataClass = parse(environment.getProperty(PROPERTY, DEFAULT.name()));
    }

    /** The environment's data class; {@link DataClass#REAL} when not declared. */
    public DataClass dataClass() {
        return dataClass;
    }

    private static DataClass parse(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return DEFAULT;
        }
        try {
            return DataClass.valueOf(trimmed.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    PROPERTY + " must be SYNTHETIC or REAL, but is \"" + value + "\"", e);
        }
    }
}
