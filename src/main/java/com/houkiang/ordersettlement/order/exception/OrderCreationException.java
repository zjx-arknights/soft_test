package com.houkiang.ordersettlement.order.exception;

import com.houkiang.ordersettlement.common.exception.BusinessException;

public class OrderCreationException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public OrderCreationException(String message) {
        super(message);
    }
}
