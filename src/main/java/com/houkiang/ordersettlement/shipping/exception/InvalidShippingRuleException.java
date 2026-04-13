package com.houkiang.ordersettlement.shipping.exception;

import com.houkiang.ordersettlement.common.exception.BusinessException;

public class InvalidShippingRuleException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public InvalidShippingRuleException(String message) {
        super(message);
    }
}
