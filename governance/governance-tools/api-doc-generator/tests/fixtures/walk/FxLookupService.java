package com.erp.fx.service;

@Service
public class FxLookupService {

    public void assertValidCode(String code) {
        if (code == null) {
            throw new LocalizedException(Status.VALIDATION_ERROR, FxErrorCodes.FX_400_BAD_TYPE, code);
        }
    }
}
