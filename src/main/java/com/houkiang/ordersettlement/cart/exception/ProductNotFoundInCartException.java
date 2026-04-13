package com.houkiang.ordersettlement.cart.exception;

import com.houkiang.ordersettlement.common.exception.BusinessException;

public class ProductNotFoundInCartException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public ProductNotFoundInCartException(String message) {
        super(message);
    }
}
