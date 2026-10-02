package com.erp.fx.controller;

@RestController
@RequestMapping("/api/v1/fx/widgets")
public class FxController {

    private final FxService fxService;

    @PostMapping
    public ResponseEntity<ApiResponse<WidgetResponse>> create(@Valid @RequestBody WidgetCreateRequest request) {
        return respond(fxService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WidgetResponse>> getById(@PathVariable Long id) {
        return respond(fxService.getById(id));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<WidgetResponse>> deactivate(@PathVariable Long id) {
        return respond(fxService.deactivate(id));
    }

    @PostMapping("/{id}/post")
    public ResponseEntity<ApiResponse<WidgetResponse>> post(@PathVariable Long id) {
        return respond(fxService.post(id));
    }

    @PostMapping("/{id}/check")
    public ResponseEntity<ApiResponse<Void>> check(@PathVariable Long id, @RequestParam(defaultValue = "x(y)") String mode) {
        fxService.check(id);
        return null;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WidgetResponse>>> list() {
        return respond(fxService.list());
    }

    private <T> ResponseEntity<ApiResponse<T>> respond(ServiceResult<T> result) {
        return null;
    }
}
