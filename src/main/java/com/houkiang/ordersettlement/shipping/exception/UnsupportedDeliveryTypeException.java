package com.houkiang.ordersettlement.shipping.exception;

import com.houkiang.ordersettlement.common.exception.BusinessException;

public class UnsupportedDeliveryTypeException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public UnsupportedDeliveryTypeException(String message) {
        super(message);
    }
}
