package com.houkiang.ordersettlement.promotion.exception;

import com.houkiang.ordersettlement.common.exception.BusinessException;

public class CouponStackNotAllowedException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public CouponStackNotAllowedException(String message) {
        super(message);
    }
}
