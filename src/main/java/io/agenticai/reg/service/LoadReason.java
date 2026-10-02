package io.agenticai.reg.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Why an item of a load run was rejected or refused: a load reason code of
 * {@link LoadReasonCodes} and the values of its message's placeholders, in order of first
 * appearance in the message. A value the load run records, never an exception (ADR-REG-011).
 *
 * @param code      the {@code REG-LOAD-*} code
 * @param arguments the placeholder values; unmodifiable, may hold {@code null} (rendered as "")
 */
record LoadReason(String code, List<Object> arguments) {

    LoadReason {
        Objects.requireNonNull(code, "code");
        arguments = arguments == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(arguments));
    }

    /** @param arguments the placeholder values in order of first appearance; a {@code null} renders as "" */
    static LoadReason of(String code, Object... arguments) {
        return new LoadReason(code, arguments == null ? List.of() : Arrays.asList(arguments));
    }
}
