package com.erp.common.web;

public class OperationCodeImpl {
    static {
        statusMappings.put(Status.NOT_FOUND, HttpStatus.NOT_FOUND);
        statusMappings.put(Status.ALREADY_EXISTS, HttpStatus.CONFLICT);
    }
}
