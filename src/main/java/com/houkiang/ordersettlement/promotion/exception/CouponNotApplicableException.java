package com.houkiang.ordersettlement.promotion.exception;

import com.houkiang.ordersettlement.common.exception.BusinessException;

public class CouponNotApplicableException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public CouponNotApplicableException(String message) {
        super(message);
    }
}
