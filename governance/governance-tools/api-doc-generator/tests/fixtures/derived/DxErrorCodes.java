package com.erp.dx.exception;

public final class DxErrorCodes {

    /**
     * RULE-DX-001 — the target period is not Open at post time. Thrown by
     * {@code DxDomain.assertPeriodOpen(...)}; the rejection clears once the period is opened.
     *
     * <p>Discussion that belongs to the constant, not to a catalogue row.
     *
     * @deprecated never
     */
    public static final String DX_409_PERIOD_NOT_OPEN = "DX-409-PERIOD-NOT-OPEN";

    public static final String DX_409_UNBALANCED = "DX-409-UNBALANCED";
}
