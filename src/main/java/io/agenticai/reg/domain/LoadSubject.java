package io.agenticai.reg.domain;

import java.util.Optional;

/**
 * What a Load Result row is about (ENT-REG-006.subjectKind). Closed: the stored values are
 * exactly those of {@code CHK_REG_LOAD_RESULT_SUBJECT_KIND}; {@code PACKAGE_DIRECTORY} is the
 * fail-safe subject of ADR-REG-018 (RULE-REG-023).
 */
public enum LoadSubject {

    SERVICE_PACKAGE("SERVICE_PACKAGE"),
    CONNECTION("CONNECTION"),
    PACKAGE_DIRECTORY("PACKAGE_DIRECTORY");

    private final String storedValue;

    LoadSubject(String storedValue) {
        this.storedValue = storedValue;
    }

    /** The value as stored in {@code SUBJECT_KIND}. */
    public String storedValue() {
        return storedValue;
    }

    /** The subject kind of a stored value; empty when the value is not one of the closed set. */
    public static Optional<LoadSubject> fromStored(String storedValue) {
        for (LoadSubject subject : values()) {
            if (subject.storedValue.equals(storedValue)) {
                return Optional.of(subject);
            }
        }
        return Optional.empty();
    }
}
