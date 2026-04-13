package com.houkiang.ordersettlement.cart.exception;

import com.houkiang.ordersettlement.common.exception.BusinessException;

public class InvalidPriceException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public InvalidPriceException(String message) {
        super(message);
    }
}
