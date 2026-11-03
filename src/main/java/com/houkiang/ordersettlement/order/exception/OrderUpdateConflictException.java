package com.houkiang.ordersettlement.order.exception;

import com.houkiang.ordersettlement.common.exception.BusinessException;

public class OrderUpdateConflictException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public OrderUpdateConflictException(String orderId) {
        super("Order status was changed by another request: " + orderId);
    }
}
