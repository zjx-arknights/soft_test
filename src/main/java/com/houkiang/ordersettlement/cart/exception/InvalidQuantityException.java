package com.houkiang.ordersettlement.cart.exception;

import com.houkiang.ordersettlement.common.exception.BusinessException;

public class InvalidQuantityException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public InvalidQuantityException(String message) {
        super(message);
    }
}
