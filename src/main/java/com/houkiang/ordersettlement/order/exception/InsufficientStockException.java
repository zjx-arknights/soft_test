package com.houkiang.ordersettlement.order.exception;

import com.houkiang.ordersettlement.common.exception.BusinessException;

public class InsufficientStockException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public InsufficientStockException(String message) {
        super(message);
    }
}
