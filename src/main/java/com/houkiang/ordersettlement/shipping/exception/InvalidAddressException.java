package com.houkiang.ordersettlement.shipping.exception;

import com.houkiang.ordersettlement.common.exception.BusinessException;

public class InvalidAddressException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public InvalidAddressException(String message) {
        super(message);
    }
}
