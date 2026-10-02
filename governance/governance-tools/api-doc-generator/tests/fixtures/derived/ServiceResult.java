package com.erp.common.domain.status;

public final class ServiceResult<T> {

    private final T data;
    private final Status status;

    private ServiceResult(T data, Status status) {
        this.data = data;
        this.status = status;
    }

    public static <T> ServiceResult<T> success(T data) {
        return new ServiceResult<>(data, Status.SUCCESS);
    }

    public static <T> ServiceResult<T> success(T data, Status status) {
        return new ServiceResult<>(data, status);
    }
}
