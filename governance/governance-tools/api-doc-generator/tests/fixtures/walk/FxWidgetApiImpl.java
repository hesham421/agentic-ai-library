package com.erp.fx.crossmodule;

/** Reached only from other modules, never from an FX controller. */
@Component
public class FxWidgetApiImpl {

    public void internal() {
        throw new LocalizedException(Status.FORBIDDEN, FxErrorCodes.FX_403_INTERNAL);
    }
}
