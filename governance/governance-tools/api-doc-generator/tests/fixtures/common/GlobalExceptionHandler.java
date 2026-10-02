package com.erp.common.web;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LocalizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocalizedException(LocalizedException ex) {
        ApiError.ApiErrorBuilder builder = ApiError.builder()
            .code(ex.getErrorCode())
            .message(resolveMessage(ex.getErrorCode(), ex.getArgs()));
        if (!ex.getErrors().isEmpty()) {
            builder.fieldErrors(ex.getErrors().stream()
                .map(detail -> FieldErrorItem.builder()
                    .field(detail.field() != null ? detail.field() : detail.errorCode())
                    .message(resolveMessage(detail.errorCode(), detail.args()))
                    .build())
                .toList());
        }
        return ResponseEntity.status(ex.getStatus().getHttpStatus()).body(ApiResponse.failure(builder.build()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldErrorItem> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> FieldErrorItem.builder()
                .field(fe.getField())
                .message(fe.getDefaultMessage())
                .build())
            .toList();
        ApiError error = ApiError.builder()
            .code(CommonErrorCodes.VALIDATION_ERROR)
            .fieldErrors(fieldErrors)
            .build();
        return ResponseEntity.badRequest().body(ApiResponse.failure(error));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> handleRequestParameter(Exception ex) {
        String parameterName = ex.getName();
        ApiError error = ApiError.builder()
            .code(CommonErrorCodes.VALIDATION_ERROR)
            .fieldErrors(List.of(FieldErrorItem.builder()
                .field(parameterName)
                .message(ex.getMessage())
                .build()))
            .build();
        return ResponseEntity.badRequest().body(ApiResponse.failure(error));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        ApiError error = ApiError.builder().code(CommonErrorCodes.ACCESS_DENIED).build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        ApiError error = ApiError.builder().code("DATA_INTEGRITY_VIOLATION").build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.failure(error));
    }
}
