package com.erp.dx.service;

@Service
public class DxService {

    private final DxRepository repository;

    public ServiceResult<WidgetResponse> create(WidgetCreateRequest request) {
        List<ErrorDetail> failures = new ArrayList<>();
        DxDomain domain = DxDomain.create();
        domain.checkPeriodOpen(request.getPeriodStatus()).ifPresent(failures::add);
        domain.checkBalanced(request.getLines()).ifPresent(failures::add);
        if (!failures.isEmpty()) {
            throw new LocalizedException(Status.CONFLICT, failures);
        }
        return ServiceResult.success(null, Status.CREATED);
    }

    public ServiceResult<WidgetResponse> loose(Long id) {
        DxDomain.create().checkBalanced(null).ifPresent(collected::add);
        return ServiceResult.success(null);
    }

    public ServiceResult<WidgetResponse> search(WidgetSearchRequest request) {
        return ServiceResult.success(null);
    }

    public ServiceResult<WidgetResponse> either(Long id) {
        if (id == null) {
            return ServiceResult.success(null, Status.CREATED);
        }
        return ServiceResult.success(null);
    }
}
