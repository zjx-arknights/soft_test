package com.houkiang.ordersettlement.catalog;

import com.houkiang.ordersettlement.common.exception.BusinessException;

public class ProductNotFoundException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public ProductNotFoundException(String productId) {
        super("Product does not exist: " + productId);
    }
}
