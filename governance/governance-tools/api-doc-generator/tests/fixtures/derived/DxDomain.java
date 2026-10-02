package com.erp.dx.domain;

public final class DxDomain {

    public static DxDomain create() {
        return new DxDomain();
    }

    public Optional<ErrorDetail> checkPeriodOpen(String statusCode) {
        if (!"OPEN".equals(statusCode)) {
            return Optional.of(ErrorDetail.of(DxErrorCodes.DX_409_PERIOD_NOT_OPEN, statusCode));
        }
        return Optional.empty();
    }

    public Optional<ErrorDetail> checkBalanced(List<Line> lines) {
        if (lines.isEmpty()) {
            return Optional.of(ErrorDetail.of(DxErrorCodes.DX_409_UNBALANCED));
        }
        return Optional.empty();
    }
}
