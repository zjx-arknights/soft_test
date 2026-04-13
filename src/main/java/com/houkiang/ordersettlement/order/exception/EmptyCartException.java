package com.houkiang.ordersettlement.order.exception;

import com.houkiang.ordersettlement.common.exception.BusinessException;

public class EmptyCartException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public EmptyCartException(String message) {
        super(message);
    }
}
