package io.agenticai.rpt.domain;

import java.util.Optional;

/**
 * The employee's decision on a COMPLETED Check — RPT's own closed list (CON-RPT-002, REQ-RPT-035;
 * column {@code RPT_CHECK_RUN.EMPLOYEE_DECISION}, CHECK constraint
 * {@code CHK_RPT_CHECK_RUN_EMPLOYEE_DECISION}). The stored values are the constant names, which a
 * consumer passes and shows as {@code VARCHAR2(30 CHAR)} text. Adding a value is a new RPT
 * version. Mapped by JPA through {@code EmployeeDecisionConverter}, which writes {@link #storedValue()} — the same values
 * {@code EnumType.STRING} would write (RPT CORE R1), but one mechanism for every RPT closed list
 * (DATA-DOM).
 */
public enum EmployeeDecision {

    APPROVED,
    REJECTED;

    /** The value as stored — the constant name. */
    public String storedValue() {
        return name();
    }

    /**
     * The value of a stored code; empty when the code is not one of the closed set.
     */
    public static Optional<EmployeeDecision> fromStored(String storedValue) {
        for (EmployeeDecision value : values()) {
            if (value.storedValue().equals(storedValue)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
