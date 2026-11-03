package com.houkiang.ordersettlement.order.exception;

import com.houkiang.ordersettlement.common.exception.BusinessException;

public class IdempotencyConflictException extends BusinessException {
    private static final long serialVersionUID = 1L;

    public IdempotencyConflictException() {
        super("Idempotency-Key was already used with a different request");
    }
}
