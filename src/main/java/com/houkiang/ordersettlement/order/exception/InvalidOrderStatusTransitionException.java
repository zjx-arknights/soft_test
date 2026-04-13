package com.houkiang.ordersettlement.order.exception;

import com.houkiang.ordersettlement.common.exception.BusinessException;

public class InvalidOrderStatusTransitionException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public InvalidOrderStatusTransitionException(String message) {
        super(message);
    }
}
