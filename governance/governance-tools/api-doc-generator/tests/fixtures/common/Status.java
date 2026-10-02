package com.erp.common.domain.status;

public enum Status {
    OK(HttpStatus.OK),
    CREATED(HttpStatus.CREATED),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    ALREADY_EXISTS(HttpStatus.CONFLICT),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    FORBIDDEN(HttpStatus.FORBIDDEN);

    private final HttpStatus httpStatus;
}
