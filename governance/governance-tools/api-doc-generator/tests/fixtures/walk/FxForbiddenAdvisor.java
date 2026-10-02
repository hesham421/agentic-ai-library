package com.erp.fx.security;

@Component
public class FxForbiddenAdvisor extends DefaultPointcutAdvisor {

    private static final String FX_SERVICE_PACKAGE = "com.erp.fx.service.";

    public FxForbiddenAdvisor() {
        setAdvice((MethodInterceptor) invocation -> {
            try {
                return invocation.proceed();
            } catch (AccessDeniedException denied) {
                throw new LocalizedException(Status.FORBIDDEN, FxErrorCodes.FX_403_FORBIDDEN);
            }
        });
    }
}
