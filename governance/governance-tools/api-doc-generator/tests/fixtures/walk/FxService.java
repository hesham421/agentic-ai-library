package com.erp.fx.service;

@Service
public class FxService {

    private final WidgetRepository repository;
    private final FxLookupService lookups;

    @PreAuthorize("hasAuthority(T(P)"
        + ".PERM_FX_CREATE)")
    public ServiceResult<WidgetResponse> create(WidgetCreateRequest request) {
        lookups.assertValidCode(request.getTypeCode());
        boolean taken = repository.existsByCode(request.getCode());
        FxDomain.create(request.getCode(), taken);
        return null;
    }

    public ServiceResult<WidgetResponse> getById(Long id) {
        Widget entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, FxErrorCodes.FX_404_WIDGET, id));
        return null;
    }

    public ServiceResult<WidgetResponse> deactivate(Long id) {
        Widget entity = requireWidget(id);
        FxDomain.from(entity).assertDeactivatable();
        return null;
    }

    public ServiceResult<WidgetResponse> post(Long id) {
        Widget entity = requireWidget(id);
        FxDomain domain = FxDomain.from(entity);
        domain.assertPostable(entity.getLines());
        return null;
    }

    public ServiceResult<List<WidgetResponse>> list() {
        return null;
    }

    public void check(String value) {
        throw new LocalizedException(Status.CONFLICT, FxErrorCodes.FX_409_CHECK_A);
    }

    public void check(Long value) {
        throw new LocalizedException(Status.CONFLICT, FxErrorCodes.FX_409_CHECK_B);
    }

    private Widget requireWidget(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND,
                FxErrorCodes.FX_404_WIDGET, id));
    }
}
