package io.agenticai.rpt.domain;

import java.util.Optional;

/**
 * How a Check obtained its documents, as RPT stores it (CON-DOC-002; columns
 * {@code RPT_CHECK_RUN.FETCH_MODE} and {@code RPT_CHECK_DOCUMENT.SOURCE_MODE}, CHECK constraints
 * {@code CHK_RPT_CHECK_RUN_FETCH_MODE} and {@code CHK_RPT_CHECK_DOCUMENT_SOURCE_MODE}). The stored
 * values are exactly {@code path | blob | manual} ({@code VARCHAR2(10 CHAR)}); no fourth fetch
 * mode exists.
 *
 * <p>RPT-side copy: RPT does not import DOC's or REG's {@code domain} package (module boundary,
 * M.1); the value arrives as a code through the Check result port and is matched with
 * {@link #fromStored(String)}.
 *
 * <p>The stored values are not the constant names, so {@code EnumType.STRING} would write
 * {@code PATH} and break the CHECK constraint: the DATA-DOM entities map this enum with an
 * {@code AttributeConverter} that writes {@link #storedValue()} (build-create-entity step 4),
 * not with {@code @Enumerated}.
 */
public enum FetchMode {

    PATH("path"),
    BLOB("blob"),
    MANUAL("manual");

    private final String storedValue;

    FetchMode(String storedValue) {
        this.storedValue = storedValue;
    }

    /** The value as stored ({@code path}, {@code blob} or {@code manual}). */
    public String storedValue() {
        return storedValue;
    }

    /**
     * The fetch mode of a stored value; empty when the value is not one of the closed set.
     */
    public static Optional<FetchMode> fromStored(String storedValue) {
        for (FetchMode mode : values()) {
            if (mode.storedValue.equals(storedValue)) {
                return Optional.of(mode);
            }
        }
        return Optional.empty();
    }
}
