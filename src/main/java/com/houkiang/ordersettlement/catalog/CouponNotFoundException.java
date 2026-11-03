package com.houkiang.ordersettlement.catalog;

import com.houkiang.ordersettlement.common.exception.BusinessException;

public class CouponNotFoundException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public CouponNotFoundException(String couponCode) {
        super("Coupon does not exist: " + couponCode);
    }
}
