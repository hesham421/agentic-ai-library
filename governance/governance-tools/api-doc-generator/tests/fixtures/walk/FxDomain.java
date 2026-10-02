package com.erp.fx.domain;

/** Domain companion; `throw new LocalizedException(Status.NOT_FOUND, FxErrorCodes.FX_IN_A_COMMENT)` is not code. */
public final class FxDomain {

    private final String code;
    private final boolean active = true;

    private FxDomain(String code) {
        if (code == null) {
            throw new LocalizedException(Status.VALIDATION_ERROR, FxErrorCodes.FX_400_CODE_REQUIRED);
        }
        this.code = code;
    }

    public static FxDomain create(String code, boolean codeTaken) {
        if (codeTaken) {
            throw new LocalizedException(Status.ALREADY_EXISTS,
                FxErrorCodes.FX_409_CODE_DUP, code);
        }
        return new FxDomain(code);
    }

    public static FxDomain from(Widget entity) {
        return new FxDomain(entity.getCode());
    }

    public void assertDeactivatable() {
        if (!active) {
            throw new LocalizedException(Status.INVALID_STATE, FxErrorCodes.FX_422_ALREADY_INACTIVE);
        }
    }

    public void assertPostable(List<Line> lines) {
        List<ErrorDetail> errors = new ArrayList<>();
        if (lines.isEmpty()) {
            errors.add(ErrorDetail.of(FxErrorCodes.FX_400_NO_LINES));
        }
        if (unbalanced(lines)) {
            errors.add(ErrorDetail.ofField("lines", FxErrorCodes.FX_400_UNBALANCED));
        }
        if (!errors.isEmpty()) {
            throw LocalizedException.withDetails(Status.VALIDATION_ERROR, FxErrorCodes.FX_400_NOT_POSTABLE, errors);
        }
        deep1();
    }

    private boolean unbalanced(List<Line> lines) {
        return false;
    }

    private void deep1() {
        deep2();
    }

    private void deep2() {
        throw new LocalizedException(Status.CONFLICT, FxErrorCodes.FX_409_TOO_DEEP);
    }
}
