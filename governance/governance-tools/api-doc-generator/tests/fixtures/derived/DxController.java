package com.erp.dx.controller;

@RestController
@RequestMapping("/api/v1/dx/widgets")
public class DxController {

    private final DxService dxService;

    @PostMapping
    public ResponseEntity<ApiResponse<WidgetResponse>> create(@Valid @RequestBody WidgetCreateRequest request) {
        return operationCode.craftResponse(dxService.create(request));
    }

    @PostMapping("/{id}/loose")
    public ResponseEntity<ApiResponse<WidgetResponse>> loose(@PathVariable Long id) {
        return operationCode.craftResponse(dxService.loose(id));
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<WidgetResponse>> search(@Valid @RequestBody WidgetSearchRequest request) {
        return operationCode.craftResponse(dxService.search(request));
    }

    @PostMapping("/{id}/either")
    public ResponseEntity<ApiResponse<WidgetResponse>> either(@PathVariable Long id) {
        return operationCode.craftResponse(dxService.either(id));
    }
}
