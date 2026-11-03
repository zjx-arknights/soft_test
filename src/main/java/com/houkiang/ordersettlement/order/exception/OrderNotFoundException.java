package com.houkiang.ordersettlement.order.exception;

import com.houkiang.ordersettlement.common.exception.BusinessException;

public class OrderNotFoundException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public OrderNotFoundException(String orderId) {
        super("Order does not exist: " + orderId);
    }
}
