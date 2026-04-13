package com.houkiang.ordersettlement.promotion.exception;

import com.houkiang.ordersettlement.common.exception.BusinessException;

public class InvalidCouponException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public InvalidCouponException(String message) {
        super(message);
    }
}
