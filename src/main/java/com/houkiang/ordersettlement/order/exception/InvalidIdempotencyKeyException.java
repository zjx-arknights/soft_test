package com.houkiang.ordersettlement.order.exception;

import com.houkiang.ordersettlement.common.exception.BusinessException;

public class InvalidIdempotencyKeyException extends BusinessException {
    private static final long serialVersionUID = 1L;

    public InvalidIdempotencyKeyException() {
        super("Idempotency-Key must contain 16 to 128 letters, digits, hyphens or underscores");
    }
}
